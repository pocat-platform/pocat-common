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

import com.rabbitmq.client.Channel;
import io.pocat.platform.message.PlatformMessageSubscription;

public class AMQPSubscription implements PlatformMessageSubscription {
    private final AMQPEndpointSubscriber amqpInboundChannel;
    private final Channel channel;
    private final String consumerTag;

    public AMQPSubscription(AMQPEndpointSubscriber amqpInboundChannel, Channel channel, String consumerTag) {
        this.amqpInboundChannel = amqpInboundChannel;
        this.channel = channel;
        this.consumerTag = consumerTag;
    }

    public void cancel() {
        this.amqpInboundChannel.cancelSubscription(consumerTag);
    }

    public Channel getChannel() {
        return channel;
    }
}
