// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.node.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbCalculateSumNodeConfiguration implements NodeConfiguration<TbCalculateSumNodeConfiguration> {

    private String inputKey;
    private String outputKey;

    @Override
    public TbCalculateSumNodeConfiguration defaultConfiguration() {
        var configuration = new TbCalculateSumNodeConfiguration();
        configuration.setInputKey("temperature");
        configuration.setOutputKey("TemperatureSum");
        return configuration;
    }

}
