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

package io.pocat.platform.message;

import java.util.Map;

public interface PlatformMessage {
    String MESSAGE_ID_HEADER_NAME = "__message_id__";
    String TOPIC_HEADER_NAME = "__topic__";
    String REPLY_TO_HEADER_NAME = "__reply_to__";
    String TX_ID_HEADER_NAME = "__tx_id__";
    String PPID_HEADER_NAME = "__ppid__";
    String PID_HEADER_NAME = "__pid__";
    String HOP_COUNT_HEADER_NAME = "__hop_count__";
    String CREATED_AT_HEADER_NAME = "__created_at__";

    String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    String CONTENT_ENCODING_HEADER_NAME = "Content-Encoding";

    String getTopic();

    byte[] getPayload();

    Map<String, Object> getHeaders();
}
