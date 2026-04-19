package com.g2rain.syncer;

import com.g2rain.common.syncer.EventMessage;
import com.g2rain.common.syncer.EventPublisherHub;
import com.g2rain.common.syncer.EventType;
import com.g2rain.common.syncer.MessageDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.SubscribableChannel;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SyncerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean("integrationConversionService", DefaultConversionService.class, DefaultConversionService::new)
        .withConfiguration(AutoConfigurations.of(SyncerAutoConfiguration.class));

    @Test
    void shouldCreateEmptyEventPublisherHubWithoutStreamBridgeAndBindings() {
        this.contextRunner
            .run(context -> {
                EventPublisherHub hub = context.getBean(EventPublisherHub.class);

                assertThat(hub.publishers()).isEmpty();
            });
    }

    @Test
    void shouldResolveOnlyOutboundBindingsWhenBindingPropertiesArePresent() throws Exception {
        SyncerAutoConfiguration autoConfiguration = new SyncerAutoConfiguration();
        Method method = SyncerAutoConfiguration.class.getDeclaredMethod(
            "resolveOutboundBindings", BindingServiceProperties.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> outboundBindings = (Set<String>) method.invoke(autoConfiguration, bindingServicePropertiesWithDirections());

        assertThat(outboundBindings).containsOnly("sync-out-0");
    }

    @Test
    void shouldNotPublishWhenNoPublisherBindingExists() {
        this.contextRunner
            .run(context -> {
                EventPublisherHub hub = context.getBean(EventPublisherHub.class);
                hub.send("sync-out-0", new EventMessage<>("basis", EventType.UPDATE, new DemoPayload(7L, "demo")));

                assertThat(hub.publishers()).isEmpty();
            });
    }

    @Test
    void shouldSubscribeOnlyConfiguredBindingChannels() {
        AtomicReference<String> dispatched = new AtomicReference<>();
        DirectChannel syncChannel = new DirectChannel();
        DirectChannel ignoredChannel = new DirectChannel();
        ignoredChannel.subscribe(message -> {
        });
        Map<String, SubscribableChannel> channels = new LinkedHashMap<>();
        channels.put("sync-in-0", syncChannel);
        channels.put("ignored-in-0", ignoredChannel);

        this.contextRunner
            .withBean(BindingServiceProperties.class, () -> bindingServiceProperties("sync-in-0"))
            .withBean("subscribableChannels", Map.class, () -> channels)
            .withBean(StreamEventSubscriber.class, () -> {
                StreamEventSubscriber subscriber = new StreamEventSubscriber(
                    provider(channels), provider(null), provider(bindingServiceProperties("sync-in-0")),
                    (MessageDispatcher) dispatched::set
                );
                return subscriber;
            })
            .run(context -> {
                StreamEventSubscriber subscriber = context.getBean(StreamEventSubscriber.class);
                subscriber.afterSingletonsInstantiated();

                syncChannel.send(org.springframework.messaging.support.MessageBuilder
                    .withPayload("{\"dataSource\":\"basis\",\"eventType\":\"CREATE\",\"data\":\"{}\"}")
                    .build());
                ignoredChannel.send(org.springframework.messaging.support.MessageBuilder
                    .withPayload("{\"dataSource\":\"ignored\",\"eventType\":\"CREATE\",\"data\":\"{}\"}")
                    .build());

                assertThat(dispatched.get()).contains("\"dataSource\":\"basis\"");
                assertThat(dispatched.get()).doesNotContain("\"dataSource\":\"ignored\"");
            });
    }

    @Test
    void shouldNotSubscribeAnyChannelWithoutBindings() {
        AtomicReference<String> dispatched = new AtomicReference<>();
        DirectChannel channel = new DirectChannel();
        channel.subscribe(message -> {
        });
        Map<String, SubscribableChannel> channels = Map.of("sync-in-0", channel);

        this.contextRunner
            .withBean("subscribableChannels", Map.class, () -> channels)
            .withBean(StreamEventSubscriber.class, () -> {
                StreamEventSubscriber subscriber = new StreamEventSubscriber(
                    provider(channels), provider(null), provider(null), (MessageDispatcher) dispatched::set
                );
                return subscriber;
            })
            .run(context -> {
                StreamEventSubscriber subscriber = context.getBean(StreamEventSubscriber.class);
                subscriber.afterSingletonsInstantiated();

                channel.send(org.springframework.messaging.support.MessageBuilder
                    .withPayload("{\"dataSource\":\"basis\",\"eventType\":\"CREATE\",\"data\":\"{}\"}")
                    .build());

                assertThat(dispatched.get()).isNull();
            });
    }

    private static BindingServiceProperties bindingServiceProperties(String... bindingNames) {
        BindingServiceProperties properties = new BindingServiceProperties();
        Map<String, BindingProperties> bindings = new LinkedHashMap<>();
        for (String bindingName : bindingNames) {
            BindingProperties bp = new BindingProperties();
            bp.setConsumer(new org.springframework.cloud.stream.binder.ConsumerProperties());
            bindings.put(bindingName, bp);
        }
        properties.setBindings(bindings);
        properties.setInputBindings(String.join(",", bindingNames));
        return properties;
    }

    private static BindingServiceProperties bindingServicePropertiesWithDirections() {
        BindingServiceProperties properties = new BindingServiceProperties();
        properties.setOutputBindings("sync-out-0");
        properties.setInputBindings("sync-in-0");

        Map<String, BindingProperties> bindings = new LinkedHashMap<>();
        BindingProperties out = new BindingProperties();
        out.setProducer(new org.springframework.cloud.stream.binder.ProducerProperties());
        bindings.put("sync-out-0", out);

        BindingProperties in = new BindingProperties();
        in.setConsumer(new org.springframework.cloud.stream.binder.ConsumerProperties());
        bindings.put("sync-in-0", in);

        properties.setBindings(bindings);
        return properties;
    }

    private static <T> org.springframework.beans.factory.ObjectProvider<T> provider(T value) {
        return new org.springframework.beans.factory.ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }
        };
    }

    private record DemoPayload(Long id, String name) {
    }
}
