// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.node.filter;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;

class TbKeyFilterNodeTest {

    DeviceId deviceId;
    TbKeyFilterNode node;
    TbKeyFilterNodeConfiguration config;
    TbContext ctx;

    @BeforeEach
    void setUp() {
        deviceId = new DeviceId(UUID.randomUUID());
        ctx = mock(TbContext.class);
        config = new TbKeyFilterNodeConfiguration().defaultConfiguration();
        config.setKey("temperature");
        node = new TbKeyFilterNode();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbKeyFilterNodeConfiguration defaultConfig = new TbKeyFilterNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getKey()).isNull();
        assertThat(defaultConfig.getFilterSource()).isEqualTo(FilterSource.DATA);
    }

    @Test
    void givenNodeClass_whenGetVersion_thenReturnsCurrentVersion() {
        assertThat(TbKeyFilterNode.class.getAnnotation(RuleNode.class).version()).isEqualTo(1);
    }

    @Test
    void givenConfigVersion0_whenInit_thenFiltersByData() throws TbNodeException {
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.toJsonNode("{\"key\":\"temperature\"}")));

        var msg = TbMsg.newMsg()
                .type(POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .data("{\"temperature\":22.5}")
                .metaData(TbMsgMetaData.EMPTY)
                .build();

        node.onMsg(ctx, msg);

        verify(ctx).tellNext(msg, "True");
        verify(ctx, never()).tellFailure(any(), any());
    }

    @ParameterizedTest
    @CsvSource({
            "DATA, '{\"temperature\":22.5}', humidity, True",
            "DATA, '{\"humidity\":40}', temperature, False",
            "METADATA, '{\"humidity\":40}', temperature, True",
            "METADATA, '{\"temperature\":22.5}', humidity, False"
    })
    void givenFilterSource_whenOnMsg_thenRoutesByKeyPresence(FilterSource filterSource, String data, String metadataKey,
                                                             String expectedConnectionType) throws TbNodeException {
        config.setFilterSource(filterSource);
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var msg = TbMsg.newMsg()
                .type(POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .data(data)
                .metaData(new TbMsgMetaData(Map.of(metadataKey, "22.5")))
                .build();

        node.onMsg(ctx, msg);

        verify(ctx).tellNext(msg, expectedConnectionType);
        verify(ctx, never()).tellFailure(any(), any());
    }

    @Test
    void givenConfigVersion0_whenUpgrade_thenFilterSourceSetToData() throws TbNodeException {
        JsonNode oldConfiguration = JacksonUtil.toJsonNode("{\"key\":\"temperature\"}");

        TbPair<Boolean, JsonNode> result = node.upgrade(0, oldConfiguration);

        assertThat(result.getFirst()).isTrue();
        assertThat(result.getSecond()).isEqualTo(JacksonUtil.toJsonNode("{\"key\":\"temperature\",\"filterSource\":\"DATA\"}"));
    }

    @Test
    void givenConfigVersion0WithFilterSource_whenUpgrade_thenNoChanges() throws TbNodeException {
        JsonNode oldConfiguration = JacksonUtil.toJsonNode("{\"key\":\"temperature\",\"filterSource\":\"METADATA\"}");

        TbPair<Boolean, JsonNode> result = node.upgrade(0, oldConfiguration);

        assertThat(result.getFirst()).isFalse();
        assertThat(result.getSecond()).isEqualTo(JacksonUtil.toJsonNode("{\"key\":\"temperature\",\"filterSource\":\"METADATA\"}"));
    }

    @Test
    void givenCurrentConfigVersion_whenUpgrade_thenNoChanges() throws TbNodeException {
        JsonNode configuration = JacksonUtil.valueToTree(config);

        TbPair<Boolean, JsonNode> result = node.upgrade(1, configuration);

        assertThat(result.getFirst()).isFalse();
        assertThat(result.getSecond()).isEqualTo(JacksonUtil.valueToTree(config));
    }

}
