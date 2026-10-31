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
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AMQPConnectionImpl implements AMQPConnection {
    private static final int DEFAULT_POOL_SIZE = 250;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final String connectionProfileName;
    private final ConnectionFactory cf;
    private Connection connection;
    private final BlockingQueue<Channel> channels;

    public AMQPConnectionImpl(String connectionProfileName, ConnectionFactory cf) {
        this.connectionProfileName = connectionProfileName;
        this.cf = cf;
        this.channels = new LinkedBlockingQueue<>(DEFAULT_POOL_SIZE);
    }

    @Override
    public Channel openChannel() throws IOException {
        Channel channel;

        while ((channel = channels.poll()) != null) {
            if (channel.isOpen()) {
                return channel;
            }
        }

        return getConnection().openChannel().orElseThrow(() -> new IOException("Failed to open channel"));
    }

    @Override
    public void releaseChannel(Channel channel) {
        if (!channel.isOpen() || !this.channels.offer(channel)) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                //ignored
            } catch (Exception e) {
                // todo Log
            }
        }
    }

    private synchronized Connection getConnection() throws IOException {
        if (this.connection == null) {
            try {
                this.connection = this.cf.newConnection();
            } catch (TimeoutException e) {
                throw new IOException("Failed to connect [" + this.connectionProfileName + "]. Timeout", e);
            } catch (IOException e) {
                throw new IOException("Failed to connect [" + this.connectionProfileName + "].", e);
            }
        }

        return this.connection;
    }

    @Override
    public void close() {
        if (this.isClosed.compareAndSet(false, true)) {
            if (this.connection != null) {
                this.channels.clear();
                try {
                    this.connection.close();
                } catch (IOException e) {
                    //ignored
                }
            }
        }
    }
}
