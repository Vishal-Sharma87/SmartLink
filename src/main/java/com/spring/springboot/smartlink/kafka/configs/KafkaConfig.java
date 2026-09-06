package com.spring.springboot.smartlink.kafka.configs;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private final KafkaProducerConfigs kafkaProducerConfigs;
    private final ConnectionConfigs connectionConfigs;

    public KafkaConfig(
            KafkaProducerConfigs kafkaProducerConfigs,
            ConnectionConfigs connectionConfigs) {

        this.kafkaProducerConfigs = kafkaProducerConfigs;
        this.connectionConfigs = connectionConfigs;
    }

    // Common producer properties

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getCommonProducerProps()));
    }

    private Map<String, Object> getCommonProducerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionConfigs.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigs.keySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigs.valueSerializer());

        props.putAll(getSaslProperties());
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(getCommonConsumerProps());
    }

    private Map<String, Object> getCommonConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionConfigs.bootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.putAll(getSaslProperties());
        return props;
        // Note: group id is not set here. With one shared factory
        // serving multiple topics/listeners, group id now belongs on each
        // @KafkaListener(groupId = "...") annotation instead
    }

    private Map<String, Object> getSaslProperties() {
        Map<String, Object> saslProperties = new HashMap<>();

        saslProperties.put("security.protocol", connectionConfigs.securityProtocol());
        saslProperties.put("sasl.mechanism", connectionConfigs.saslMechanism());
        saslProperties.put("sasl.jaas.config", connectionConfigs.saslJaasConfig());
        saslProperties.put("session.timeout.ms", connectionConfigs.sessionTimeoutMs());

        return saslProperties;
    }

    @Bean
    public StringJsonMessageConverter kafkaJsonMessageConverter() {
        return new StringJsonMessageConverter();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordMessageConverter(kafkaJsonMessageConverter());
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));
        return factory;
    }

}
