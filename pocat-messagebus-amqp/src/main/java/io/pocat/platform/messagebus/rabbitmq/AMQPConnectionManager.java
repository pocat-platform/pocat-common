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
import com.rabbitmq.client.ConnectionFactory;
import io.pocat.platform.messagebus.EndpointContext;
import io.pocat.platform.messagebus.RefCountedResourceManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class AMQPConnectionManager extends RefCountedResourceManager<AMQPConnection> {
    private static final String HOST_ARG_NAME = "host";
    private static final String PORT_ARG_NAME = "port";
    private static final String USER_ARG_NAME = "user";
    private static final String PASSWORD_ARG_NAME = "password";
    private static final String VHOST_ARG_NAME = "vhost";
    private static final String URI_ARG_NAME = "uri";
    private static final String CONNECTION_TIMEOUT_ARG_NAME = "connection-timeout";

    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    AMQPConnectionManager() {
        super(AMQPConnectionWrapper::new);
    }

    public AMQPConnection getAMQPConnection(EndpointContext endpointContext) {
        return super.get(
                endpointContext.getName(),
                () -> createAMQPConnection(endpointContext)
        );
    }

    private AMQPConnection createAMQPConnection(EndpointContext endpointContext) {
        Map<String, String> args = endpointContext.getConnectionProfile().getArgs();
        ConnectionFactory cf = new ConnectionFactory();
        cf.setAutomaticRecoveryEnabled(true);
        cf.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        for (String name : args.keySet()) {
            switch (name) {
                case HOST_ARG_NAME:
                    cf.setHost(args.get(name));
                    break;
                case PORT_ARG_NAME:
                    cf.setPort(Integer.parseInt(args.get(name)));
                    break;
                case USER_ARG_NAME:
                    cf.setUsername(args.get(name));
                    break;
                case PASSWORD_ARG_NAME:
                    cf.setPassword(args.get(name));
                    break;
                case VHOST_ARG_NAME:
                    cf.setVirtualHost(args.get(name));
                    break;
                case URI_ARG_NAME:
                    try {
                        cf.setUri(args.get(name));
                    } catch (Exception e) {
                        throw new UncheckedIOException(new IOException("Failed to create uri [" + args.get(name) + "]"));
                    }
                    break;
                case CONNECTION_TIMEOUT_ARG_NAME:
                    cf.setConnectionTimeout(Integer.parseInt(args.get(name)));
                    break;
                default:
                    // ignore argument
            }
        }
        return new AMQPConnectionImpl(endpointContext.getConnectionProfile().getName(), cf);
    }

    private static class AMQPConnectionWrapper extends RefCountedResourceManager.ResourceWrapper<AMQPConnection> implements AMQPConnection {
        public AMQPConnectionWrapper(AMQPConnection resource, Runnable releaser) {
            super(resource, releaser);
        }

        @Override
        public Channel openChannel() throws IOException {
            return getResource().openChannel();
        }

        @Override
        public void releaseChannel(Channel channel) {
            getResource().releaseChannel(channel);
        }
    }
}
