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

import io.pocat.platform.message.PlatformMessagePublisher;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class MessageBusPublisherFactory extends MessageBusBase<PlatformMessagePublisher> {
    private static final Pattern PUBLISH_TOPIC_VALIDATE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]+(?:\\.[a-zA-Z0-9\\-]+)*$");
    private static final Pattern SUBSCRIBE_TOPIC_VALIDATE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-\\.]+:(?!(.*\\{[a-zA-Z0-9\\-_]+\\+}\\.))([a-zA-Z0-9\\-]+|\\{[a-zA-Z0-9\\-_]+\\+?})(?:\\.([a-zA-Z0-9\\-]+|\\{[a-zA-Z0-9\\-_]+\\+?}))*$");

    protected MessageBusPublisherFactory(MessageBusContext messageBusContext) {
        super(messageBusContext, RefCountedPlatformMessagePublisher::new);
    }

    protected PlatformMessagePublisher createPublisher(String endpointName) {
        EndpointContext endpointContext = getMessageBusContext().getEndpointContext(endpointName);
        if (endpointContext == null) {
            throw new IllegalArgumentException("Not found endpoint [" + endpointName + "]");
        }
        return MessageBusEndpointFactoryProvider.getInstance().createPublisher(endpointContext);
    }

    public PlatformMessagePublisher getPublisher(String endpoint) {
        return get(endpoint, () -> createPublisher(endpoint));
    }

    private static class RefCountedPlatformMessagePublisher extends ResourceWrapper<PlatformMessagePublisher> implements PlatformMessagePublisher {
        private RefCountedPlatformMessagePublisher(PlatformMessagePublisher channel, Runnable releaser) {
            super(channel, releaser);
        }

        @Override
        public void publish(String topic, Map<String, Object> headers, byte[] payload) throws IOException {
            getResource().publish(topic, headers, payload);
        }
    }
}
