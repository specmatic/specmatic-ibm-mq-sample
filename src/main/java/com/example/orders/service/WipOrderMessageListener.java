package com.example.orders.service;

import com.example.orders.config.AppProperties;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.listeners.wip-orders", name = "enabled", havingValue = "true")
public class WipOrderMessageListener {

  private static final Logger log = LoggerFactory.getLogger(WipOrderMessageListener.class);

  private final OrderProcessingService orderProcessingService;
  private final AppProperties appProperties;

  public WipOrderMessageListener(
      OrderProcessingService orderProcessingService,
      AppProperties appProperties) {
    this.orderProcessingService = orderProcessingService;
    this.appProperties = appProperties;
  }

  @JmsListener(destination = "${app.queues.wip-orders}")
  public void onWipOrders(Message message) {
    try {
      orderProcessingService.onWipOrder(
          OrderMessageListener.asText(message),
          OrderMessageListener.getCorrelationId(message));
    } catch (Exception ex) {
      log.error("Failed processing message from {}", appProperties.queues().wipOrders(), ex);
    }
  }
}
