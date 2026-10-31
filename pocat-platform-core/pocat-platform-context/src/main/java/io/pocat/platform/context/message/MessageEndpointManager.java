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

package io.pocat.platform.context.message;

import io.pocat.platform.context.PlatformComponent;
import io.pocat.platform.message.PlatformMessagePublisherFactory;
import io.pocat.platform.message.PlatformMessageSubscriberFactory;

import java.util.HashMap;
import java.util.Map;

public class MessageEndpointManager implements PlatformComponent {
    private final Map<String, PlatformMessageSubscriberFactory> subscriberFactories = new HashMap<>();
    private final Map<String, PlatformMessagePublisherFactory> publisherFactories = new HashMap<>();

    public PlatformMessageSubscriberFactory getMessageSubscriber(String protocol) {
        return this.subscriberFactories.get(protocol);
    }

    public PlatformMessagePublisherFactory getMessagePublisherFactory(String protocol) {
        return this.publisherFactories.get(protocol);
    }

    public void register(String protocol, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Message subscriber or publisher factory must not be null.");
        }
        if (this.subscriberFactories.containsKey(protocol)) {
            throw new IllegalStateException("Message subscriber factory already registered for protocol [" + protocol + "]");
        }
        if (this.publisherFactories.containsKey(protocol)) {
            throw new IllegalStateException("Message publisher factory already registered for protocol [" + protocol + "]");
        }

        if (obj instanceof PlatformMessageSubscriberFactory) {
            this.subscriberFactories.put(protocol, (PlatformMessageSubscriberFactory) obj);
        }

        if (obj instanceof PlatformMessagePublisherFactory) {
            this.publisherFactories.put(protocol, (PlatformMessagePublisherFactory) obj);
        }
    }

    @Override
    public void destroy() {
        for (PlatformMessagePublisherFactory publisher : this.publisherFactories.values()) {
            publisher.close();
        }

        for (PlatformMessageSubscriberFactory subscriber : this.subscriberFactories.values()) {
            subscriber.close();
        }
    }
}
