// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.node.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.FILTER,
        name = "check key",
        version = 1,
        relationTypes = {"True", "False"},
        configClazz = TbKeyFilterNodeConfiguration.class,
        nodeDescription = "Checks the existence of the selected key in the message data or metadata.",
        nodeDetails = "If the selected key exists - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbFilterNodeCheckKeyConfig"
)
public class TbKeyFilterNode implements TbNode {

    private static final String FILTER_SOURCE = "filterSource";

    private String key;
    private FilterSource filterSource;

    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbKeyFilterNodeConfiguration.class);
        key = config.getKey();
        filterSource = config.getFilterSource();
        if (filterSource == null) {
            throw new TbNodeException("Filter source must be specified!", true);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        boolean hasKey = switch (filterSource) {
            case DATA -> JacksonUtil.toJsonNode(msg.getData()).has(key);
            case METADATA -> msg.getMetaData().getValue(key) != null;
        };
        ctx.tellNext(msg, hasKey ? TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.hasNonNull(FILTER_SOURCE)) {
                    ((ObjectNode) oldConfiguration).put(FILTER_SOURCE, FilterSource.DATA.name());
                    hasChanges = true;
                }
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
