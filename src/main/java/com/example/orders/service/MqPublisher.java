package com.example.orders.service;

import jakarta.jms.JMSException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class MqPublisher {

  private static final String ORDER_CORRELATION_ID = "orderCorrelationId";

  private final JmsTemplate jmsTemplate;

  public MqPublisher(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  public void publish(String destination, String payload, String orderCorrelationId) {
    jmsTemplate.send(destination, session -> {
      var message = session.createTextMessage(payload);
      if (orderCorrelationId != null && !orderCorrelationId.isBlank()) {
        try {
          message.setStringProperty(ORDER_CORRELATION_ID, orderCorrelationId);
        } catch (JMSException ex) {
          throw new IllegalStateException("Unable to set orderCorrelationId header", ex);
        }
      }
      return message;
    });
  }
}
