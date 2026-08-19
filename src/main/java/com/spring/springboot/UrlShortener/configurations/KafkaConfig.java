package com.spring.springboot.UrlShortener.configurations;

import com.spring.springboot.UrlShortener.model.LinkAnalysisDto;
import com.spring.springboot.UrlShortener.model.LinkCreationDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.sasl.jaas.config}")
    private String saslJaasConfig;

    @Value("${spring.kafka.properties.security.protocol}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.session.timeout.ms}")
    private int sessionTimeoutMs;

    // Common producer properties
    private Map<String, Object> getCommonProducerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonSerializer");
        props.put("security.protocol", securityProtocol);
        props.put("sasl.mechanism", saslMechanism);
        props.put("sasl.jaas.config", saslJaasConfig);
        props.put("session.timeout.ms", sessionTimeoutMs);
        return props;
    }

    // Producer Factories (keep as is)
    @Bean
    public ProducerFactory<String, LinkCreationDto> linkCreationProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerProps());
    }

    @Bean
    public ProducerFactory<String, LinkAnalysisDto> linkAnalysisProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerProps());
    }

    @Bean
    public KafkaTemplate<String, LinkCreationDto> linkCreationKafkaTemplate() {
        return new KafkaTemplate<>(linkCreationProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, LinkAnalysisDto> linkAnalysisKafkatemplate() {
        return new KafkaTemplate<>(linkAnalysisProducerFactory());
    }

    // Consumer Factory for LinkCreationDto
    @Bean
    public ConsumerFactory<String, LinkCreationDto> linkCreationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "link-creation-group");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.spring.springboot.UrlShortener.model");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LinkCreationDto.class);

        // SASL/SSL properties
        props.put("security.protocol", securityProtocol);
        props.put("sasl.mechanism", saslMechanism);
        props.put("sasl.jaas.config", saslJaasConfig);
        props.put("session.timeout.ms", sessionTimeoutMs);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // Consumer Factory for LinkAnalysisDto
    @Bean
    public ConsumerFactory<String, LinkAnalysisDto> linkAnalysisConsumerFactory() {
            Map<String, Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "link-analysis-group");
            props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.spring.springboot.UrlShortener.model");
            props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
            props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, LinkAnalysisDto.class);

            // SASL/SSL properties
            props.put("security.protocol", securityProtocol);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config", saslJaasConfig);
            props.put("session.timeout.ms", sessionTimeoutMs);

            return new DefaultKafkaConsumerFactory<>(props);
    }



    // Container Factory for LinkCreation
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LinkCreationDto> linkCreationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LinkCreationDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(linkCreationConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));
        return factory;
    }

    // Container Factory for LinkAnalysis
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LinkAnalysisDto> linkAnalysisKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LinkAnalysisDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(linkAnalysisConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));
        return factory;
    }
}
