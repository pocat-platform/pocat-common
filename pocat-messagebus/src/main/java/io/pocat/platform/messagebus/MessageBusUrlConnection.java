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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageBusUrlConnection extends URLConnection {
    private final String endpoint;
    private final String topic;
    private final MessageBusFactory factory;
    private final Map<String, String> headers = new HashMap<>();
    private final OutputStream outputStream;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     * @param factory
     */
    protected MessageBusUrlConnection(URL url, MessageBusFactory factory) {
        super(url);
        this.factory = factory;
        this.endpoint = url.getHost();
        this.topic = url.getPath();
        this.outputStream = new MessageBusOutputStream(this);
    }

    @Override
    public void connect() throws IOException {

    }

    @Override
    public void setRequestProperty(String key, String value) {
        this.headers.put(key, value);
    }

    @Override
    public void addRequestProperty(String key, String value) {
        this.headers.put(key, value);
    }

    @Override
    public String getRequestProperty(String key) {
        return this.headers.get(key);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return this.outputStream;
    }

    private Map<String, String> getHeaders() {
        return this.headers;
    }

    private void publish(Map<String, Object> headers, byte[] payload) throws IOException {
        String topic = this.topic;
        if(topic.startsWith("/")) {
            topic = topic.substring(1);
        }
        this.factory.getEndpointPublisher(this.endpoint).publish(topic, headers, payload);
    }

    private static class MessageBusOutputStream extends OutputStream {
        private final MessageBusUrlConnection con;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        private MessageBusOutputStream(MessageBusUrlConnection con) {
            this.con = con;
        }

        @Override
        public void write(int b) throws IOException {
            this.baos.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            this.baos.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.baos.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {
            this.con.publish(Collections.unmodifiableMap(this.con.getHeaders()), this.baos.toByteArray());
        }
    }
}
