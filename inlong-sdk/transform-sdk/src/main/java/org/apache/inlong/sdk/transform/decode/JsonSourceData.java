/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.transform.decode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * JsonSourceData
 * 
 */
public class JsonSourceData implements SourceData {

    public static final String ROOT_KEY = "$root";

    public static final String CHILD_KEY = "$child";

    private JsonObject root;

    private JsonArray childRoot;

    /**
     * Constructor
     * @param root
     * @param childRoot
     */
    public JsonSourceData(JsonObject root, JsonArray childRoot) {
        this.root = root;
        this.childRoot = childRoot;
    }

    /**
     * getRowCount
     * @return
     */
    @Override
    public int getRowCount() {
        if (this.childRoot == null) {
            return 1;
        } else {
            return this.childRoot.size();
        }
    }

    /**
     * getField
     * @param rowNum
     * @param fieldName
     * @return
     */
    @Override
    public String getField(int rowNum, String fieldName) {
        try {
            List<JsonNode> childNodes = new ArrayList<>();
            String[] nodeStrings = fieldName.split("\\.");
            for (String nodeString : nodeStrings) {
                childNodes.add(new JsonNode(nodeString));
            }
            // parse
            if (childNodes.size() == 0) {
                return "";
            }
            // first node
            JsonNode firstNode = childNodes.get(0);
            JsonElement current = root;
            if (StringUtils.equals(ROOT_KEY, firstNode.getName())) {
                current = root;
            } else if (StringUtils.equals(CHILD_KEY, firstNode.getName())) {
                if (rowNum < childRoot.size()) {
                    current = childRoot.get(rowNum);
                } else {
                    return "";
                }
            } else {
                // error data
                return "";
            }
            if (current == null) {
                // error data
                return "";
            }
            // parse other node
            for (int i = 1; i < childNodes.size(); i++) {
                JsonNode node = childNodes.get(i);
                if (!current.isJsonObject()) {
                    // error data
                    return "";
                }
                JsonElement newElement = current.getAsJsonObject().get(node.getName());
                if (newElement == null) {
                    // error data
                    return "";
                }
                // node is not array
                if (!node.isArray()) {
                    current = newElement;
                    continue;
                }
                // node is an array
                if (!newElement.isJsonArray()) {
                    // error data
                    return "";
                }
                JsonArray newArray = newElement.getAsJsonArray();
                if (node.getArrayIndex() >= newArray.size()) {
                    // error data
                    return "";
                }
                current = newArray.get(node.getArrayIndex());
            }
            return current.getAsString();
        } catch (Exception e) {
            return "";
        }
    }
}
