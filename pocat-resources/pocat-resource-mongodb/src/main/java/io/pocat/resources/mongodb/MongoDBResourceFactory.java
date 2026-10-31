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

import io.pocat.resources.Resource;
import io.pocat.resources.ResourceConfig;
import io.pocat.resources.ResourceFactory;

public class MongoDBResourceFactory implements ResourceFactory {
    private static final String MONGODB_NAME = "mongodb";
    private static final String MONGODB_SHORT_NAME = "mongo";
    @Override
    public Resource<?> createResource(ResourceConfig config) {
        return new MongoDBResource(config);
    }

    @Override
    public boolean supports(String type) {
        return MONGODB_NAME.equalsIgnoreCase(type) || MONGODB_SHORT_NAME.equalsIgnoreCase(type);
    }
}
