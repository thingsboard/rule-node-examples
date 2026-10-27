// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.node.filter;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbKeyFilterNodeConfiguration implements NodeConfiguration<TbKeyFilterNodeConfiguration> {

    private String key;

    @Override
    public TbKeyFilterNodeConfiguration defaultConfiguration() {
        var configuration = new TbKeyFilterNodeConfiguration();
        configuration.setKey(null);
        return configuration;
    }

}
