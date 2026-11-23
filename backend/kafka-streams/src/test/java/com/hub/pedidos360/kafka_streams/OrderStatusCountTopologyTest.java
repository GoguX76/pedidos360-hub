package com.hub.pedidos360.kafka_streams;

import com.hub.pedidos360.messaging.kafka.OrderEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusCountTopologyTest {

    @Test
    void publishesCumulativeEventCountsByStatus() {
        StreamsBuilder builder = new StreamsBuilder();
        OrderStatusCountTopology.build(builder, "orders-in", "counts-out");

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-status-count-test");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");

        try (TopologyTestDriver driver = new TopologyTestDriver(builder.build(), config)) {
            TestInputTopic<String, OrderEvent> input = driver.createInputTopic(
                    "orders-in",
                    Serdes.String().serializer(),
                    OrderStatusCountTopology.orderEventSerde().serializer());
            TestOutputTopic<String, Long> output = driver.createOutputTopic(
                    "counts-out",
                    Serdes.String().deserializer(),
                    Serdes.Long().deserializer());

            input.pipeInput("42", event("CREATED"));
            assertCount(output.readRecord(), "CREATED", 1L);

            input.pipeInput("42", event("CONFIRMED"));
            assertCount(output.readRecord(), "CONFIRMED", 1L);

            input.pipeInput("42", event("CREATED"));
            assertCount(output.readRecord(), "CREATED", 2L);
            assertThat(output.isEmpty()).isTrue();
        }
    }

    private OrderEvent event(String status) {
        return new OrderEvent(
                "event-" + status,
                OrderEvent.TYPE_STATUS_CHANGED,
                42L,
                "customer-1",
                "Test Customer",
                1L,
                "DELIVERY",
                status,
                new BigDecimal("10.00"),
                List.of(new OrderEvent.Item(1L, "Test product", 1, new BigDecimal("10.00"))),
                "2026-11-23T09:00:00");
    }

    private void assertCount(TestRecord<String, Long> record, String status, long count) {
        assertThat(record.key()).isEqualTo(status);
        assertThat(record.value()).isEqualTo(count);
    }
}
