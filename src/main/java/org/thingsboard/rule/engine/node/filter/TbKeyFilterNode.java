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
package org.thingsboard.rule.engine.node.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;


@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check key",
        relationTypes = {"True", "False"},
        configClazz = TbKeyFilterNodeConfiguration.class,
        nodeDescription = "Checks the existence of the selected key in the message payload.",
        nodeDetails = "If the selected key  exists - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbFilterNodeCheckKeyConfig")
public class TbKeyFilterNode implements TbNode {

    private TbKeyFilterNodeConfiguration config;
    private String key;


    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbKeyFilterNodeConfiguration.class);
        key = config.getKey();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        ctx.tellNext(msg, JacksonUtil.toJsonNode(msg.getData()).has(key) ?
                TbNodeConnectionType.TRUE : TbNodeConnectionType.FALSE);
    }

    @Override
    public void destroy() {
    }
}
