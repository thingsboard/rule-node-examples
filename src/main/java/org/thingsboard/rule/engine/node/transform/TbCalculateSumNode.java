/**
 * Copyright © 2018 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

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

    private static final ObjectMapper mapper = new ObjectMapper();

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
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        double sum = 0;
        boolean hasRecords = false;
        try {
            JsonNode jsonNode = mapper.readTree(msg.getData());
            Iterator<String> iterator = jsonNode.fieldNames();
            while (iterator.hasNext()) {
                String field = iterator.next();
                if (field.startsWith(inputKey)) {
                    hasRecords = true;
                    sum += jsonNode.get(field).asDouble();
                }
            }
            if (hasRecords) {
                TbMsg newMsg = TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), mapper.writeValueAsString(mapper.createObjectNode().put(outputKey, sum)));
                ctx.tellSuccess(newMsg);
            } else {
                ctx.tellFailure(msg, new TbNodeException("Message doesn't contains the key: " + inputKey));
            }
        } catch (IOException e) {
            ctx.tellFailure(msg, e);
        }
    }

    long getNow() {
        return System.currentTimeMillis();
    }

    @Override
    public void destroy() {
    }
}
