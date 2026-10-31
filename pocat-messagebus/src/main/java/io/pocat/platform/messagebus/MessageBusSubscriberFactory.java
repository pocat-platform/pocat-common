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

package io.pocat.platform.messagebus;

import io.pocat.platform.message.PlatformMessageListener;
import io.pocat.platform.message.PlatformMessageSubscriber;
import io.pocat.platform.message.PlatformMessageSubscription;

import java.io.IOException;

public class MessageBusSubscriberFactory extends MessageBusBase<PlatformMessageSubscriber> {

    public MessageBusSubscriberFactory(MessageBusContext messageBusContext) {
        super(messageBusContext, RefCountedEndpointSubscriber::new);
    }

    public PlatformMessageSubscriber getSubscriber(String endpoint) {
        return get(endpoint, () -> createChannel(endpoint));
    }

    protected PlatformMessageSubscriber createChannel(String endpointName) {
        EndpointContext endpointContext = getMessageBusContext().getEndpointContext(endpointName);
        if (endpointContext == null) {
            throw new IllegalArgumentException("Not found endpoint [" + endpointName + "]");
        }
        return MessageBusEndpointFactoryProvider.getInstance().createSubscriber(endpointContext);
    }

    private static class RefCountedEndpointSubscriber extends ResourceWrapper<PlatformMessageSubscriber> implements PlatformMessageSubscriber {
        private RefCountedEndpointSubscriber(PlatformMessageSubscriber channel, Runnable releaser) {
            super(channel, releaser);
        }

        @Override
        public PlatformMessageSubscription subscribe(String groupName, String topic, PlatformMessageListener handler) throws IOException {
            return getResource().subscribe(groupName, topic, handler);
        }
    }
}