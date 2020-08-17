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
package org.thingsboard.rule.engine.node.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

/**
 * Created by mshvayka on 10.08.18.
 */
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "get sum into metadata",
        configClazz = TbGetSumIntoMetadataConfiguration.class,
        nodeDescription = "Calculate Sum of the telemetry data, which fields begin with the specified prefix and add the result into Message Metadata ",
        nodeDetails = "If fields in Message payload start with the <code>Input Key</code>, Sum of this fields added into metadata.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbEnrichmentNodeSumIntoMetadataConfig")
public class TbGetSumIntoMetadata implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbGetSumIntoMetadataConfiguration config;
    private String inputKey;
    private String outputKey;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetSumIntoMetadataConfiguration.class);
        inputKey = config.getInputKey();
        outputKey = config.getOutputKey();
    }


    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        double sum = 0;
        boolean hasRecords = false;
        try {
            JsonNode jsonNode = mapper.readTree(msg.getData());
            Iterator<String> iterator = jsonNode.fieldNames();
            while (iterator.hasNext()) {
                String field = iterator.next();
                if (field.startsWith(inputKey)) {
                    sum += jsonNode.get(field).asDouble();
                    hasRecords = true;
                }
            }
            if (hasRecords) {
                msg.getMetaData().putValue(outputKey, Double.toString(sum));
                ctx.tellNext(msg, SUCCESS);
            } else {
                ctx.tellFailure(msg, new Exception("Message doesn't contains the Input Key: " + inputKey));
            }
        } catch (IOException e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void destroy() {

    }
}
