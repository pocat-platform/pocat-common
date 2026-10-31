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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import io.pocat.platform.message.PlatformMessagePublisher;
import io.pocat.platform.messagebus.EndpointContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AMQPPlatformMessagePublisher extends AMQPEndpoint implements PlatformMessagePublisher {
    private static final AMQPConnectionManager OUTBOUND_CONNECTION_MANAGER = new AMQPConnectionManager();
    private final AMQPConnection connection;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public AMQPPlatformMessagePublisher(EndpointContext endpointContext) {
        super(endpointContext);
        this.connection = OUTBOUND_CONNECTION_MANAGER.getAMQPConnection(getEndpointContext());
    }

    @Override
    public void publish(String topic, Map<String, Object> headers, byte[] payload) throws IOException {
        checkClosed();
        if (topic.contains(DOT_REPLACER)) {
            throw new IllegalArgumentException(DOT_REPLACER + " is reserved. Not allowed.");
        }
        String normalizedTopic = normalizeTopic(topic);
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().headers(new HashMap<>(headers)).build();
        Channel channel = this.connection.openChannel();
        channel.basicPublish(getExchangeName(), normalizedTopic, properties, payload);
        this.connection.releaseChannel(channel);
    }

    private void checkClosed() {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Already closed");
        }
    }

    @Override
    public void close() {
        if (this.isClosed.compareAndSet(false, true)) {
            this.connection.close();
        }
    }
}
