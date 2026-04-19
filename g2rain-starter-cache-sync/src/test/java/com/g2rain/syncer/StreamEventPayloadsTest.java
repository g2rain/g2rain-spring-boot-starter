package com.g2rain.syncer;

import com.g2rain.common.syncer.EventMessage;
import com.g2rain.common.syncer.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class StreamEventPayloadsTest {

    @Test
    void shouldSerializeEventMessageToDispatcherRawJson() {
        EventMessage<TestPayload> message = new EventMessage<>(
            "basis", EventType.UPDATE, new TestPayload(1L, "name")
        );

        String rawMessage = StreamEventPayloads.toRawMessage(message);

        assertThat(rawMessage).contains("\"dataSource\":\"basis\"");
        assertThat(rawMessage).contains("\"eventType\":\"UPDATE\"");
        assertThat(rawMessage).contains("\\\"id\\\":1");
        assertThat(rawMessage).contains("\\\"name\\\":\\\"name\\\"");
    }

    @Test
    void shouldExtractRawMessageFromStringPayload() {
        String raw = "{\"dataSource\":\"basis\",\"eventType\":\"CREATE\",\"data\":\"{}\"}";

        assertThat(StreamEventPayloads.extractRawMessage(MessageBuilder.withPayload(raw).build()))
            .isEqualTo(raw);
    }

    @Test
    void shouldExtractRawMessageFromEventMessagePayload() {
        EventMessage<TestPayload> message = new EventMessage<>(
            "basis", EventType.CREATE, new TestPayload(2L, "demo")
        );

        String rawMessage = StreamEventPayloads.extractRawMessage(MessageBuilder.withPayload(message).build());

        assertThat(rawMessage).contains("\"dataSource\":\"basis\"");
        assertThat(rawMessage).contains("\\\"id\\\":2");
    }

    private record TestPayload(Long id, String name) {
    }
}
