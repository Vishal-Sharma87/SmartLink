package com.spring.springboot.smartlink.kafka.services;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, Object event) {
        kafkaTemplate.send(topic, event);
    }
}
