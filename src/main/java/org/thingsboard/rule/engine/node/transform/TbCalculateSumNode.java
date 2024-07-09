/**
 * Copyright © 2018-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.node.transform;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Iterator;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "calculate sum",
        configClazz = TbCalculateSumNodeConfiguration.class,
        nodeDescription = "Calculates Sum of the telemetry data, which fields begin with the specified prefix. ",
        nodeDetails = "If fields in Message payload start with the <code>Input Key</code>, the Sum of these fields is added to the new Message payload.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbTransformationNodeSumConfig")
public class TbCalculateSumNode implements TbNode {

    TbCalculateSumNodeConfiguration config;
    String inputKey;
    String outputKey;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCalculateSumNodeConfiguration.class);
        inputKey = config.getInputKey();
        outputKey = config.getOutputKey();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        double sum = 0;
        boolean hasRecords = false;
        JsonNode data = JacksonUtil.toJsonNode(msg.getData());
        Iterator<String> iterator = data.fieldNames();
        while (iterator.hasNext()) {
            String field = iterator.next();
            if (field.startsWith(inputKey)) {
                hasRecords = true;
                sum += data.get(field).asDouble();
            }
        }
        if (hasRecords) {
            var newDataWithSum = JacksonUtil.newObjectNode();
            TbMsg newMsg = TbMsg.transformMsgData(msg, JacksonUtil.toString(newDataWithSum.put(outputKey, sum)));
            ctx.tellSuccess(newMsg);
        } else {
            ctx.tellFailure(msg, new TbNodeException("Message doesn't contains the key: " + inputKey));
        }
    }

    long getNow() {
        return System.currentTimeMillis();
    }

    @Override
    public void destroy() {
    }
}
