package com.hub.pedidos360.kafka_streams;

import com.hub.pedidos360.messaging.kafka.OrderEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@Configuration
@EnableKafkaStreams
public class OrderStatusCountTopology {

    @Bean
    KStream<String, Long> orderStatusCounts(
            StreamsBuilder streamsBuilder,
            @Value("${app.kafka.order-events-topic}") String inputTopic,
            @Value("${app.kafka.order-status-counts-topic}") String outputTopic) {
        return build(streamsBuilder, inputTopic, outputTopic);
    }

    static KStream<String, Long> build(
            StreamsBuilder streamsBuilder,
            String inputTopic,
            String outputTopic) {
        Serde<String> stringSerde = Serdes.String();
        Serde<OrderEvent> orderEventSerde = orderEventSerde();

        KStream<String, OrderEvent> events = streamsBuilder.stream(
                inputTopic,
                Consumed.with(stringSerde, orderEventSerde));

        KTable<String, Long> counts = events
                .filter((orderId, event) -> event != null
                        && event.status() != null
                        && !event.status().isBlank())
                .groupBy((orderId, event) -> event.status(),
                        Grouped.with(stringSerde, orderEventSerde))
                .count(Materialized.with(stringSerde, Serdes.Long()));

        KStream<String, Long> updates = counts.toStream();
        updates.to(outputTopic, Produced.with(stringSerde, Serdes.Long()));
        return updates;
    }

    static Serde<OrderEvent> orderEventSerde() {
        return Serdes.serdeFrom(
                new JacksonJsonSerializer<OrderEvent>().noTypeInfo(),
                new JacksonJsonDeserializer<>(OrderEvent.class).ignoreTypeHeaders());
    }
}
