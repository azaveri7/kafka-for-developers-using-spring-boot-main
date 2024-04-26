package com.paathshala.config;

import com.paathshala.service.FailureService;
import com.paathshala.service.LibraryEventsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;

import java.util.List;

@Configuration
@EnableKafka
@Slf4j
public class LibraryEventsConsumerConfig {

    @Autowired
    KafkaTemplate kafkaTemplate;

    @Value("${topics.retry:library-events.RETRY}")
    private String retryTopic;

    @Value("${topics.dlt:library-events.DLT}")
    private String deadLetterTopic;

    public DeadLetterPublishingRecoverer publishingRecoverer() {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate
                , (r, e) -> {
            if (e.getCause() instanceof RecoverableDataAccessException) {
                return new TopicPartition(retryTopic, r.partition());
            } else {
                return new TopicPartition(deadLetterTopic, r.partition());
            }
        }
        );

        return recoverer;

    }

    public DefaultErrorHandler errorHandler() {
        var exceptionsToIgnoreList = List.of(
                IllegalArgumentException.class
        );
        var exceptionsToRetryList = List.of(
                RecoverableDataAccessException.class
        );

        var fixedBackOff = new FixedBackOff(1000L, 2);
        // Fixed vs exponential backoff
        /*var exponentialBackOff = new ExponentialBackOffWithMaxRetries(2);
        exponentialBackOff.setInitialInterval(1_000L);
        exponentialBackOff.setMultiplier(2.0);
        exponentialBackOff.setMaxMultiplier(2_000L);*/
        var errorHandler = new DefaultErrorHandler(
                publishingRecoverer(),
                fixedBackOff);
        //var errorHandler = new DefaultErrorHandler(exponentialBackOff);
        exceptionsToIgnoreList.forEach(
            errorHandler::addNotRetryableExceptions
        );
        exceptionsToRetryList.forEach(
            //errorHandler::addNotRetryableExceptions
            errorHandler::addRetryableExceptions
        );

        errorHandler.addNotRetryableExceptions();
        errorHandler.setRetryListeners((((record, ex, deliveryAttempt) -> {
            log.info("Failed Record in Retry Listener, Exception : {}, deliveryAttempt : {}",
                    ex.getMessage(), deliveryAttempt);
        }))
        );
        return errorHandler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.setCommonErrorHandler(errorHandler());
       /* configurer.configure(factory, kafkaConsumerFactory
                .getIfAvailable(() ->
                        new DefaultKafkaConsumerFactory<>(this.kafkaProperties.buildConsumerProperties())));*/
        //factory.setConcurrency(3);
        //factory.setCommonErrorHandler(errorHandler());
        configurer.configure(factory, kafkaConsumerFactory);
        //factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}