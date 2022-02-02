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
package org.thingsboard.rule.engine.node.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.msg.session.SessionMsgType.POST_ATTRIBUTES_REQUEST;

@Slf4j
class TbCalculateSumNodeTest {
    public static final long NOW_TS = 1632831917545L; // Tue Sep 28 2021 15:25:17 GMT+0300

    final ObjectMapper mapper = new ObjectMapper();

    DeviceId deviceId;
    TbCalculateSumNode node;
    TbCalculateSumNodeConfiguration config;
    TbNodeConfiguration nodeConfiguration;
    TbContext ctx;
    TbMsgCallback callback;

    @BeforeEach
    void setUp() throws TbNodeException {
        deviceId = new DeviceId(UUID.randomUUID());
        callback = mock(TbMsgCallback.class);
        ctx = mock(TbContext.class);
        config = new TbCalculateSumNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
        node = spy(new TbCalculateSumNode());
        willReturn(NOW_TS).given(node).getNow(); //mocking current time

        node.init(ctx, nodeConfiguration);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() {
        assertThat(node.inputKey).isEqualTo("temperature");
        assertThat(node.outputKey).isEqualTo("TemperatureSum");
        assertThat(node.config).isEqualTo(config);
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        TbCalculateSumNodeConfiguration defaultConfig = new TbCalculateSumNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getInputKey()).isEqualTo("temperature");
        assertThat(defaultConfig.getOutputKey()).isEqualTo("TemperatureSum");
    }

    @Test
    void givenMsg_whenOnMsg_thenVerifyOutput() throws Exception {
        final Map<String, String> mdMap = Map.of(
                "country", "US",
                "city", "NY"
        );
        final TbMsgMetaData metaData = new TbMsgMetaData(mdMap);
        final String data = "{\"temperature1\":22.5,\"temperature2\":10.3}";
        final String expected = "{\"TemperatureSum\":32.8}";

        TbMsg msg = TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", deviceId, metaData, data, callback);

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellSuccess(newMsgCaptor.capture());
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();

        log.info("data: {}", newMsg);
        assertThat(newMsg.getType()).isEqualTo(POST_ATTRIBUTES_REQUEST.name());
        assertThat(newMsg.getMetaData().getData()).isEqualTo(mdMap);
        assertThat(newMsg.getData()).isEqualTo(expected);
    }

    @Test
    void givenEmptyMsg_whenOnMsg_thenTellFailure() throws Exception {
        final TbMsgMetaData metaData = new TbMsgMetaData();
        final String data = "{}";
        TbMsg msg = TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", deviceId, metaData, data, callback);

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(ctx, never()).tellSuccess(any());
        verify(ctx, times(1)).tellFailure(newMsgCaptor.capture(), exceptionCaptor.capture());

        assertThat(newMsgCaptor.getValue()).isSameAs(msg);
        assertThat(exceptionCaptor.getValue()).isInstanceOf(TbNodeException.class);
        assertThat(exceptionCaptor.getValue().getMessage()).startsWith("Message doesn't contains the key: ");
        assertThat(exceptionCaptor.getValue().getMessage()).contains("temperature");
    }

}
