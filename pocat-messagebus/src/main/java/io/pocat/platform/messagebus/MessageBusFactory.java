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
import io.pocat.platform.message.PlatformMessagePublisherFactory;
import io.pocat.platform.message.PlatformMessageSubscriber;
import io.pocat.platform.message.PlatformMessageSubscriberFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class MessageBusFactory implements PlatformMessagePublisherFactory, PlatformMessageSubscriberFactory {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final MessageBusPublisherFactory publisherFactory;
    private final MessageBusSubscriberFactory subscriberFactory;

    public MessageBusFactory(MessageBusContext context) {
        this.publisherFactory = new MessageBusPublisherFactory(context);
        this.subscriberFactory = new MessageBusSubscriberFactory(context);
    }

    @Override
    public PlatformMessagePublisher getEndpointPublisher(String endpoint) {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Already closed");
        }
        return this.publisherFactory.getPublisher(endpoint);
    }

    @Override
    public PlatformMessageSubscriber getSubscriber(String endpoint) {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Already closed");
        }
        return this.subscriberFactory.getSubscriber(endpoint);
    }

    @Override
    public void close() {
        if (this.isClosed.compareAndSet(false, true)) {
            this.publisherFactory.close();
            this.subscriberFactory.close();
        }
    }
}
