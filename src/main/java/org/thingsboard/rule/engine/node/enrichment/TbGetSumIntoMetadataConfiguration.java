/**
 * Copyright © 2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.node.enrichment;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Created by mshvayka on 10.08.18.
 */
@Data
public class TbGetSumIntoMetadataConfiguration implements NodeConfiguration<TbGetSumIntoMetadataConfiguration> {

    private String inputKey;
    private String outputKey;


    @Override
    public TbGetSumIntoMetadataConfiguration defaultConfiguration() {
        TbGetSumIntoMetadataConfiguration configuration = new TbGetSumIntoMetadataConfiguration();
        configuration.setInputKey("temperature");
        configuration.setOutputKey("TemperatureSum");
        return configuration;
    }
}
