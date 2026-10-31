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

package io.pocat.resources.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.pocat.resources.Resource;
import io.pocat.resources.ResourceConfig;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class MongoDBResource implements Resource<MongoDatabase> {
    private final String name;
    private final MongoClient mongoClient;
    private final String databaseName;

    public MongoDBResource(ResourceConfig config) {
        this.name = config.getName();
        ConnectionString cs = buildConnectionString(config.getProperties());
        this.databaseName = cs.getDatabase();
        if(this.databaseName == null || this.databaseName.isBlank()) {
            throw new IllegalArgumentException("Database name is not set");
        }
        this.mongoClient = MongoClients.create(cs);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public MongoDatabase getInstance() {
        return this.mongoClient.getDatabase(this.databaseName);
    }

    @Override
    public void close() {
        this.mongoClient.close();
    }

    private ConnectionString buildConnectionString(Map<String, String> properties) {
        String scheme = "mongodb";
        String host = "localhost";
        String port = "27017";
        String database = null;
        String username = null;
        String password = null;

        Map<String, String> options = new LinkedHashMap<>();

        String uriValue = properties.get("uri");
        if (uriValue != null && !uriValue.isBlank()) {
            URI uri = URI.create(uriValue);

            scheme = uri.getScheme();
            host = uri.getHost();

            if (uri.getPort() >= 0) {
                port = String.valueOf(uri.getPort());
            }

            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                database = path.substring(1);
            }

            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                int separator = userInfo.indexOf(':');

                if (separator >= 0) {
                    username = userInfo.substring(0, separator);
                    password = userInfo.substring(separator + 1);
                } else {
                    username = userInfo;
                }
            }

            String query = uri.getQuery();
            if (query != null && !query.isBlank()) {
                for (String parameter : query.split("&")) {
                    int separator = parameter.indexOf('=');

                    if (separator >= 0) {
                        options.put(
                                parameter.substring(0, separator),
                                parameter.substring(separator + 1));
                    } else {
                        options.put(parameter, "");
                    }
                }
            }
        }

        host = properties.getOrDefault("host", host);
        port = properties.getOrDefault("port", port);
        database = properties.getOrDefault("database", database);
        username = properties.getOrDefault("username", username);
        password = properties.getOrDefault("password", password);

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String name = entry.getKey();

            if (!"uri".equals(name) && !"host".equals(name) && !"port".equals(name) && !"database".equals(name) && !"username".equals(name) && !"password".equals(name)) {
                options.put(name, entry.getValue());
            }
        }

        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("MongoDB database is required");
        }

        StringBuilder value = new StringBuilder(scheme).append("://");

        if (username != null) {
            value.append(username);

            if (password != null) {
                value.append(':').append(password);
            }

            value.append('@');
        }

        value.append(host);

        if (port != null && !port.isBlank()) {
            value.append(':').append(port);
        }

        value.append('/').append(database);

        if (!options.isEmpty()) {
            value.append('?');

            boolean first = true;
            for (Map.Entry<String, String> entry : options.entrySet()) {
                if (!first) {
                    value.append('&');
                }

                value.append(entry.getKey()).append('=').append(entry.getValue());

                first = false;
            }
        }

        return new ConnectionString(value.toString());
    }
}
