package com.kosasih.tsmart.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.kosasih.tsmart.IntegrationTest;
import com.kosasih.tsmart.config.EmbeddedKafka;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.MimeTypeUtils;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
@EmbeddedKafka
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
class TsmartKafkaResourceIT {

    @Autowired
    private MockMvc restMockMvc;

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @Test
    void producesMessages() throws Exception {
        restMockMvc.perform(post("/api/tsmart-kafka/publish?message=value-produce")).andExpect(status().isOk());
        assertThat(output.receive(1000, "binding-out-0").getPayload()).isEqualTo("value-produce".getBytes());
    }

    @Test
    void producesPooledMessages() throws Exception {
        assertThat(output.receive(1500, "kafkaProducer-out-0").getPayload()).isEqualTo("kakfa_producer".getBytes());
    }

    @Test
    void consumesMessages() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE);
        MessageHeaders headers = new MessageHeaders(map);
        Message<String> testMessage = new GenericMessage<>("value-consume", headers);
        MvcResult mvcResult = restMockMvc
            .perform(get("/api/tsmart-kafka/register"))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted())
            .andReturn();
        for (int i = 0; i < 100; i++) {
            input.send(testMessage);
            Thread.sleep(100);
            String content = mvcResult.getResponse().getContentAsString();
            if (content.contains("data:value-consume")) {
                restMockMvc.perform(get("/api/tsmart-kafka/unregister"));
                return;
            }
        }
        fail("Expected content data:value-consume not received");
    }
}
