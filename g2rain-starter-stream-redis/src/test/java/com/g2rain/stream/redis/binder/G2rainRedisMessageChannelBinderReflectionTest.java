package com.g2rain.stream.redis.binder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.messaging.MessageChannel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class G2rainRedisMessageChannelBinderReflectionTest {

    private G2rainRedisMessageChannelBinder binder;

    @BeforeEach
    void setUp() throws Exception {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        this.binder = new G2rainRedisMessageChannelBinder(connectionFactory, "traceId", "tenantId");
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("integrationEvaluationContext", new StandardEvaluationContext());
        binder.setApplicationContext(context);
    }

    @Test
    void shouldContainCustomHeadersWithoutChangingBusinessCode() throws Exception {
        Field headersField = G2rainRedisMessageChannelBinder.class.getDeclaredField("headersToMap");
        headersField.setAccessible(true);
        String[] headers = (String[]) headersField.get(binder);

        assertTrue(Arrays.asList(headers).contains("traceId"));
        assertTrue(Arrays.asList(headers).contains("tenantId"));
    }

    @Test
    void shouldEnableRetryChannelOnlyWhenMaxAttemptsGreaterThanOne() throws Exception {
        Method retryMethod = G2rainRedisMessageChannelBinder.class.getDeclaredMethod(
            "addRetryIfNeeded", String.class, DirectChannel.class, ConsumerProperties.class
        );
        retryMethod.setAccessible(true);

        DirectChannel bridge = new DirectChannel();
        ConsumerProperties disabled = new ConsumerProperties();
        disabled.setMaxAttempts(1);
        ConsumerProperties enabled = new ConsumerProperties();
        enabled.setMaxAttempts(3);

        MessageChannel disabledResult = (MessageChannel) retryMethod.invoke(binder, "basis.sync.groupA", bridge, disabled);
        MessageChannel enabledResult = (MessageChannel) retryMethod.invoke(binder, "basis.sync.groupA", bridge, enabled);

        assertSame(bridge, disabledResult);
        assertNotSame(bridge, enabledResult);
    }

    @Test
    void shouldCreatePartitionedProducerEndpoint() throws Exception {
        Method producerEndpointMethod = G2rainRedisMessageChannelBinder.class.getDeclaredMethod(
            "createProducerEndpoint", String.class, ProducerProperties.class
        );
        producerEndpointMethod.setAccessible(true);

        ProducerProperties partitioned = new ProducerProperties();
        partitioned.setPartitionCount(3);
        partitioned.setPartitionKeyExpression(new SpelExpressionParser().parseExpression("payload"));
        partitioned.setPartitionKeyExtractorName("partitionKeyExtractor");
        assertTrue(partitioned.isPartitioned());
        RedisQueueOutboundChannelAdapter adapter = (RedisQueueOutboundChannelAdapter) producerEndpointMethod.invoke(
            binder, "basis.sync.groupA", partitioned
        );

        Field expressionField = RedisQueueOutboundChannelAdapter.class.getDeclaredField("queueNameExpression");
        expressionField.setAccessible(true);
        Object expression = expressionField.get(adapter);

        assertNotNull(adapter);
        assertNotNull(expression);
    }
}
