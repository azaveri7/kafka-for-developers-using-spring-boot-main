package com.paathshala.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paathshala.domain.LibraryEvent;
import util.TestUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
public class LibraryEventsControllerIntegrationTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    // Configure embeddedKafkaBroker
    /*
    Add below annotations

    @EmbeddedKafka(topics = {"library-events"}, partitions = 3)
    @TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
     */
    // Override the kafka producer bootstrap address to the embedded broker ones

    // Configure a Kafka consumer in the test case
    // Wire KafkaConsumer and EmbeddedKafkaBroker
    // Consume the record from the EmbeddedKafkaBroker and then assert on it

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    ObjectMapper objectMapper;

    private Consumer<Integer, String> consumer;

    @BeforeEach
    void setUp() {
        var configs = new HashMap<>(KafkaTestUtils
                .consumerProps("group1", "true", embeddedKafkaBroker));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(configs,
                new IntegerDeserializer(), new StringDeserializer())
                .createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

   @AfterEach
   void tearDown() {
        consumer.close();
   }

    @Test
    void postLibraryEvent() {
        //given
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type", MediaType.APPLICATION_JSON.toString());
        var httpEntity = new HttpEntity<>(TestUtil.libraryEventRecord(), httpHeaders);
        //when
        var responseEntity = testRestTemplate
                .exchange("/v1/libraryevent", HttpMethod.POST,
                        httpEntity, LibraryEvent.class);
        //then
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        ConsumerRecords<Integer, String> consumerRecords =
                KafkaTestUtils.getRecords(consumer);
        assert consumerRecords.count() == 1;

        consumerRecords.forEach( record -> {
           var libraryEventActual = TestUtil.parseLibraryEventRecord(objectMapper, record.value());
           System.out.println("Actual library event fetched from Kafka"
                   + libraryEventActual);
           assertEquals(libraryEventActual, TestUtil.libraryEventRecord());
        });

    }
}
