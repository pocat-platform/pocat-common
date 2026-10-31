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

import io.pocat.platform.messagebus.EndpointContext;

import java.util.Map;

public class AMQPEndpoint {
    protected static final String DOT_REPLACER = "__DOT__";
    protected static final String EXCHANGE_ARG_NAME = "exchange";
    private final String exchangeName;
    private final EndpointContext endpointContext;

    protected AMQPEndpoint(EndpointContext endpointContext) {
        this.endpointContext = endpointContext;
        Map<String, String> endpointArgs = endpointContext.getArgs();
        this.exchangeName = endpointArgs.get(EXCHANGE_ARG_NAME);
        if (this.exchangeName == null) {
            throw new IllegalArgumentException("Exchange args does not exist");
        }
    }

    protected String normalizeTopic(String topic) {
        String normalizedTopic = topic;
        if (normalizedTopic.startsWith("/")) {
            normalizedTopic = normalizedTopic.substring(1);
        }
        return normalizedTopic.replace(".", DOT_REPLACER).replace("/", ".");
    }

    protected String rollbackTopic(String normalizedTopic) {
        return normalizedTopic.replace(".", "/").replace(DOT_REPLACER, ".");
    }

    protected EndpointContext getEndpointContext() {
        return endpointContext;
    }

    protected String getExchangeName() {
        return this.exchangeName;
    }
}
