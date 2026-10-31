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
import com.rabbitmq.client.Delivery;
import io.pocat.platform.message.PlatformMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AMQPPlatformMessage implements PlatformMessage {
    private static final String REPLY_TO_HEADER = "__reply_to__";

    private final String topic;
    private final byte[] payload;
    private final Map<String, Object> headers;

    public AMQPPlatformMessage(String topic, Delivery delivery) {
        this.topic = topic;
        Map<String, Object> headers;
        if (delivery.getProperties() == null || delivery.getProperties().getHeaders() == null) {
            headers = Collections.emptyMap();
        } else {
            headers = new HashMap<>();
            AMQP.BasicProperties properties = delivery.getProperties();
            if (properties != null) {
                if (properties.getMessageId() != null) {
                    headers.put(MESSAGE_ID_HEADER_NAME,
                            properties.getMessageId());
                }

                if (properties.getCorrelationId() != null) {
                    headers.put(TX_ID_HEADER_NAME,
                            properties.getCorrelationId());
                }

                if (properties.getTimestamp() != null) {
                    headers.put(CREATED_AT_HEADER_NAME,
                            properties.getTimestamp().getTime());
                }

                if (properties.getReplyTo() != null) {
                    headers.put(REPLY_TO_HEADER,
                            properties.getReplyTo());
                }

                if (properties.getContentType() != null) {
                    headers.put("contentType", properties.getContentType());
                }

                if (properties.getContentEncoding() != null) {
                    headers.put("contentEncoding", properties.getContentEncoding());
                }

                if (properties.getType() != null) {
                    headers.put("type", properties.getType());
                }

                if (properties.getHeaders() != null) {
                    headers.putAll(properties.getHeaders());
                }
            }

            headers.putAll(delivery.getProperties().getHeaders());
            Object replyTo = headers.get(REPLY_TO_HEADER);
            if (replyTo != null && !(replyTo instanceof String)) {
                headers.put(REPLY_TO_HEADER, String.valueOf(replyTo));
            }
        }
        this.headers = headers;
        this.payload = delivery.getBody();
    }

    @Override
    public String getTopic() {
        return this.topic;
    }

    @Override
    public byte[] getPayload() {
        return this.payload;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return this.headers;
    }
}
