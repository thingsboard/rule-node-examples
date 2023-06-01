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
package org.thingsboard.rule.engine.node.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbVersionedNode;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;


@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check key",
        version = 1,
        relationTypes = {"True", "False"},
        configClazz = TbKeyFilterNodeConfiguration.class,
        nodeDescription = "Checks the existence of the selected key in the message or message metadata.",
        nodeDetails = "If the selected key  exists - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbFilterNodeCheckKeyConfig")
public class TbKeyFilterNode implements TbVersionedNode {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbKeyFilterNodeConfiguration config;
    private String key;


    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbKeyFilterNodeConfiguration.class);
        key = config.getKey();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            ctx.tellNext(msg, mapper.readTree(msg.getData()).has(key) ? "True" : "False");
        } catch (IOException e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        try {
            if (fromVersion == 0) {
                var newConfigObjectNode = (ObjectNode) oldConfiguration;
                newConfigObjectNode.put("filterSource", FilterSource.DATA.name());
                return new TbPair<>(true, newConfigObjectNode);
            }
            return new TbPair<>(false, oldConfiguration);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }
}
