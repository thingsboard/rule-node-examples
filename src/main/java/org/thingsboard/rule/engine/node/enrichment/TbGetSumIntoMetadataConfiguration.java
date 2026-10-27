// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.node.enrichment;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbGetSumIntoMetadataConfiguration implements NodeConfiguration<TbGetSumIntoMetadataConfiguration> {

    private String inputKey;
    private String outputKey;

    @Override
    public TbGetSumIntoMetadataConfiguration defaultConfiguration() {
        var configuration = new TbGetSumIntoMetadataConfiguration();
        configuration.setInputKey("temperature");
        configuration.setOutputKey("TemperatureSum");
        return configuration;
    }

}
