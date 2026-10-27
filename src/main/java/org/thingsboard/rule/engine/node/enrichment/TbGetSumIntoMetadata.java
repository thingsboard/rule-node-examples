// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.node.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.thingsboard.server.common.data.msg.TbNodeConnectionType.SUCCESS;

@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "get sum into metadata",
        configClazz = TbGetSumIntoMetadataConfiguration.class,
        nodeDescription = "Calculate Sum of the telemetry data, which fields begin with the specified prefix and add the result into Message Metadata ",
        nodeDetails = "If fields in Message payload start with the <code>Input Key</code>, Sum of this fields added into metadata.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbEnrichmentNodeSumIntoMetadataConfig"
)
public class TbGetSumIntoMetadata implements TbNode {

    private String inputKey;
    private String outputKey;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbGetSumIntoMetadataConfiguration.class);
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
                sum += data.get(field).asDouble();
                hasRecords = true;
            }
        }
        if (hasRecords) {
            msg.getMetaData().putValue(outputKey, Double.toString(sum));
            ctx.tellNext(msg, SUCCESS);
        } else {
            ctx.tellFailure(msg, new Exception("Message doesn't contains the Input Key: " + inputKey));
        }
    }

}
