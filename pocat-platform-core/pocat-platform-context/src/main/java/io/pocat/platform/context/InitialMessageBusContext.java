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

package io.pocat.platform.context;

import io.pocat.platform.env.EnvContext;
import io.pocat.platform.env.EnvironmentProvider;
import io.pocat.platform.messagebus.ClusterConnectionProfile;
import io.pocat.platform.messagebus.EndpointContext;
import io.pocat.platform.messagebus.MessageBusContext;
import io.pocat.platform.util.xml.XmlDigester;
import io.pocat.platform.util.xml.annotation.XmlElementWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InitialMessageBusContext implements MessageBusContext {
    private static final String MESSAGE_BUS_CONTEXT_HOME_PATH = "/env/contexts/messagebus";
    private static EnvironmentProvider ENV_PROVIDER;
    private static MessageBusContext DEFAULT_CONTEXT;

    public InitialMessageBusContext(EnvironmentProvider envProvider) {
        synchronized (InitialMessageBusContext.class) {
            if (DEFAULT_CONTEXT == null) {
                InitialMessageBusContext.ENV_PROVIDER = envProvider;
                DEFAULT_CONTEXT = createDefaultContext(envProvider);
            } else if (InitialMessageBusContext.ENV_PROVIDER != envProvider) {
                throw new IllegalStateException("Different EnvironmentProvider is not supported.");
            }
        }
    }

    private MessageBusContext createDefaultContext(EnvironmentProvider provider) {
        MessageBusContextImpl messageBusContext = new MessageBusContextImpl();
        EnvContext context = provider.lookup(MESSAGE_BUS_CONTEXT_HOME_PATH);

        EnvContext conHomeCtx = context.getChild("connections");
        List<EnvContext> conCtxs = conHomeCtx.getDescendants();

        Map<String, ClusterConnectionProfile> profiles = new HashMap<>();
        for (EnvContext conCtx : conCtxs) {
            if (conCtx.hasData()) {
                try (InputStream conStream = conCtx.openDataStream()) {
                    ClusterConnectionProfile profile = ClusterConnectionProfile.parse(conStream);
                    profiles.put(profile.getName(), profile);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to load connection profile [" + conCtx.getPath() + "]");
                }
            }
        }

        EnvContext endpointHomeCtx = context.getChild("endpoints");
        List<EnvContext> endpointInfoCtxs = endpointHomeCtx.getDescendants();
        for (EnvContext endpointInfoCtx : endpointInfoCtxs) {
            if (endpointInfoCtx.hasData()) {
                try (InputStream endpointStream = endpointInfoCtx.openDataStream()) {
                    EndpointInfo endpointInfo = EndpointInfo.parse(endpointStream);
                    EndpointContext endpointCtx = new EndpointContextImpl(endpointInfo, profiles);
                    messageBusContext.addEndpointCtx(endpointCtx);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to load endpoint profile [" + endpointInfoCtx.getPath() + "]", e);
                }
            }
        }

        return messageBusContext;
    }

    @Override
    public EndpointContext getEndpointContext(String endpointName) {
        return DEFAULT_CONTEXT.getEndpointContext(endpointName);
    }

    private static class EndpointInfo {
        private static final XmlDigester XML_DIGESTER = XmlDigester.newBuilder().build();
        private String name;
        private String connectionProfileId;
        @XmlElementWrapper("args")
        private Map<String, String> args;

        public static EndpointInfo parse(InputStream endpointStream) {
            return XML_DIGESTER.digest(endpointStream, EndpointInfo.class);
        }

        public String getName() {
            return this.name;
        }

        public String getConnectionProfileId() {
            return connectionProfileId;
        }

        public Map<String, String> getArgs() {
            return this.args;
        }

    }

    private static class MessageBusContextImpl implements MessageBusContext {
        private final Map<String, EndpointContext> endpointContexts = new HashMap<>();

        @Override
        public EndpointContext getEndpointContext(String endpointName) {
            return this.endpointContexts.get(endpointName);
        }

        public void addEndpointCtx(EndpointContext endpointCtx) {
            this.endpointContexts.put(endpointCtx.getName(), endpointCtx);
        }
    }

    private static class EndpointContextImpl implements EndpointContext {
        private final EndpointInfo endpointInfo;
        private final Map<String, ClusterConnectionProfile> profiles;

        public EndpointContextImpl(EndpointInfo endpointInfo, Map<String, ClusterConnectionProfile> profiles) {
            this.endpointInfo = endpointInfo;
            this.profiles = profiles;
        }

        @Override
        public String getName() {
            return this.endpointInfo.getName();
        }

        @Override
        public ClusterConnectionProfile getConnectionProfile() {
            return this.profiles.get(this.endpointInfo.getConnectionProfileId());
        }

        @Override
        public Map<String, String> getArgs() {
            return this.endpointInfo.getArgs();
        }
    }
}
