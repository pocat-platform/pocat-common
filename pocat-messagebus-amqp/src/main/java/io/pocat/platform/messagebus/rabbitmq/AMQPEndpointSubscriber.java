/*
 * Copyright 2024. dongobi soft inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pocat.platform.messagebus.rabbitmq;

import com.rabbitmq.client.*;
import io.pocat.platform.message.PlatformMessageSubscriber;
import io.pocat.platform.message.PlatformMessageListener;
import io.pocat.platform.messagebus.EndpointContext;
import io.pocat.platform.message.PlatformMessageSubscription;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AMQPEndpointSubscriber extends AMQPEndpoint implements PlatformMessageSubscriber {

    private static final AMQPConnectionManager INBOUND_CONNECTION_MANAGER = new AMQPConnectionManager();
    private static final int DEFAULT_PREFETCH_COUNT = 100;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private final Map<String, AMQPSubscription> subscriptions = new ConcurrentHashMap<>();
    private final AMQPConnection connection;

    public AMQPEndpointSubscriber(EndpointContext endpointContext) {
        super(endpointContext);
        this.connection = INBOUND_CONNECTION_MANAGER.getAMQPConnection(getEndpointContext());
    }

    @Override
    public PlatformMessageSubscription subscribe(String groupName, final String topic, PlatformMessageListener handler) throws IOException {
        //replace topic / to .
        if (this.isClosed.get()) {
            throw new IllegalStateException("Already closed");
        }

        String normalizedTopic = normalizeTopic(topic);

        this.readLock.lock();
        boolean isSubscribed = false;
        Channel channel = null;
        try {
            if (this.isClosed.get()) {
                throw new IllegalStateException("Already closed");
            }
            channel = this.connection.openChannel();
            channel.basicQos(DEFAULT_PREFETCH_COUNT);
            channel.exchangeDeclare(getExchangeName(), BuiltinExchangeType.TOPIC, true, false, null);
            channel.queueDeclare(groupName, true, false, true, null);
            channel.queueBind(groupName, getExchangeName(), normalizedTopic);
            String consumerTag = channel.basicConsume(groupName, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    handleMessage(getChannel(), new Delivery(envelope, properties, body), handler);
                }

                @Override
                public void handleCancel(String consumerTag) throws IOException {
                    // todo Logging serverside callback
                    cancelSubscription(consumerTag);
                }
            });
            AMQPSubscription subscription = new AMQPSubscription(this, channel, consumerTag);
            this.subscriptions.put(consumerTag, subscription);
            isSubscribed = true;
            return subscription;
        } finally {
            this.readLock.unlock();
            if (channel != null) {
                if (!isSubscribed) {
                    this.connection.releaseChannel(channel);
                }
            }
        }
    }

    private void handleMessage(Channel subscribeChannel, Delivery delivery, PlatformMessageListener handler) {
        long deliveryTag = delivery.getEnvelope().getDeliveryTag();
        String topic;
        if (delivery.getEnvelope().getRoutingKey() == null || delivery.getEnvelope().getRoutingKey().isBlank()) {
            topic = "";
        } else {
            topic = rollbackTopic(delivery.getEnvelope().getRoutingKey());
        }
        handler.handleMessage(new AMQPPlatformMessage(topic, delivery))
                .thenRun(() -> ack(subscribeChannel, deliveryTag));
    }

    private void ack(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            //ignored
        }
    }

    @Override
    public void close() {
        if (this.isClosed.compareAndSet(false, true)) {
            this.writeLock.lock();
            List<PlatformMessageSubscription> subscriptions;
            try {
                subscriptions = new ArrayList<>(this.subscriptions.values());
                this.subscriptions.clear();
                this.connection.close();
            } finally {
                this.writeLock.unlock();
            }
            subscriptions.forEach(PlatformMessageSubscription::cancel);
        }
    }

    public void cancelSubscription(String consumerTag) {
        AMQPSubscription subscription = this.subscriptions.remove(consumerTag);
        if (subscription != null) {
            Channel channel = subscription.getChannel();
            try {
                channel.basicCancel(consumerTag);
            } catch (IOException e) {
                //ignored
            }
            this.connection.releaseChannel(channel);
        }
    }
}
