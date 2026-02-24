package com.example.orders.service;

import com.example.orders.config.AppProperties;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class OrderMessageListener {

  private static final Logger log = LoggerFactory.getLogger(OrderMessageListener.class);
  private static final String ORDER_CORRELATION_ID = "orderCorrelationId";

  private final OrderProcessingService orderProcessingService;
  private final AppProperties appProperties;

  public OrderMessageListener(OrderProcessingService orderProcessingService, AppProperties appProperties) {
    this.orderProcessingService = orderProcessingService;
    this.appProperties = appProperties;
  }

  @JmsListener(destination = "${app.queues.new-orders}")
  public void onNewOrders(Message message) {
    try {
      orderProcessingService.onNewOrder(asText(message), getCorrelationId(message));
    } catch (Exception ex) {
      log.error("Failed processing message from {}", appProperties.queues().newOrders(), ex);
    }
  }

  @JmsListener(destination = "${app.queues.cancel-requests}")
  public void onCancelRequests(Message message) {
    try {
      orderProcessingService.onCancelRequest(asText(message), getCorrelationId(message));
    } catch (Exception ex) {
      log.error("Failed processing message from {}", appProperties.queues().cancelRequests(), ex);
    }
  }

  @JmsListener(destination = "${app.queues.out-for-delivery-orders}")
  public void onOutForDeliveryOrders(Message message) {
    try {
      orderProcessingService.onOutForDelivery(asText(message));
    } catch (Exception ex) {
      log.error("Failed processing message from {}", appProperties.queues().outForDeliveryOrders(), ex);
    }
  }

  static String asText(Message message) throws JMSException {
    if (message.isBodyAssignableTo(String.class)) {
      return message.getBody(String.class);
    }
    if (message instanceof TextMessage textMessage) {
      return textMessage.getText();
    }
    if (message instanceof BytesMessage bytesMessage) {
      bytesMessage.reset();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = bytesMessage.readBytes(buffer)) != -1) {
        output.write(buffer, 0, read);
      }
      return output.toString(StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException("Only TextMessage or BytesMessage is supported");
  }

  static String getCorrelationId(Message message) throws JMSException {
    return message.propertyExists(ORDER_CORRELATION_ID)
        ? message.getStringProperty(ORDER_CORRELATION_ID)
        : null;
  }
}
