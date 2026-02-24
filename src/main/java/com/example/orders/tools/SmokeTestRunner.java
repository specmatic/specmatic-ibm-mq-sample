package com.example.orders.tools;

import com.example.orders.config.AppProperties;
import com.example.orders.service.JsonService;
import com.example.orders.service.MqPublisher;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.smoke-test", name = "enabled", havingValue = "true")
public class SmokeTestRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(SmokeTestRunner.class);

  private final MqPublisher publisher;
  private final JmsTemplate jmsTemplate;
  private final JsonService jsonService;
  private final AppProperties properties;
  private final ConfigurableApplicationContext context;

  public SmokeTestRunner(
      MqPublisher publisher,
      JmsTemplate jmsTemplate,
      JsonService jsonService,
      AppProperties properties,
      ConfigurableApplicationContext context) {
    this.publisher = publisher;
    this.jmsTemplate = jmsTemplate;
    this.jsonService = jsonService;
    this.properties = properties;
    this.context = context;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      runScenario();
      log.info("Smoke test PASSED");
      System.exit(org.springframework.boot.SpringApplication.exit(context, () -> 0));
    } catch (Exception ex) {
      log.error("Smoke test FAILED", ex);
      System.exit(org.springframework.boot.SpringApplication.exit(context, () -> 1));
    }
  }

  private void runScenario() {
    String correlationId = "smoke-" + System.currentTimeMillis();

    Map<String, Object> newOrder = Map.of(
        "id", 101,
        "orderItems", new Object[] {
          Map.of("id", 1, "name", "Keyboard", "quantity", 1, "price", 120.50),
          Map.of("id", 2, "name", "Mouse", "quantity", 2, "price", 35.00)
        });
    publisher.publish(properties.queues().newOrders(), jsonService.write(newOrder), correlationId);

    Message acceptedMessage = jmsTemplate.receiveSelected(
        properties.queues().acceptedOrders(), "orderCorrelationId = '" + correlationId + "'");
    String acceptedPayload = requireText(acceptedMessage, properties.queues().acceptedOrders());
    log.info("Received accepted-orders: {}", acceptedPayload);

    Map<String, Object> cancel = Map.of("id", 101);
    publisher.publish(properties.queues().cancelRequests(), jsonService.write(cancel), correlationId);
    Message cancelledMessage = jmsTemplate.receiveSelected(
        properties.queues().cancelledOrders(), "orderCorrelationId = '" + correlationId + "'");
    String cancelledPayload = requireText(cancelledMessage, properties.queues().cancelledOrders());
    log.info("Received cancelled-orders: {}", cancelledPayload);

    Map<String, Object> outForDelivery = Map.of(
        "orderId", 101,
        "deliveryAddress", "123 Main Street, Springfield",
        "deliveryDate", LocalDate.now().plusDays(1).toString());
    publisher.publish(properties.queues().outForDeliveryOrders(), jsonService.write(outForDelivery), correlationId);
  }

  private String requireText(Message message, String queueName) {
    if (message == null) {
      throw new IllegalStateException("No message received from " + queueName + " within timeout");
    }
    try {
      if (message instanceof TextMessage textMessage) {
        return textMessage.getText();
      }
      throw new IllegalStateException("Unexpected message type from " + queueName + ": " + message.getClass());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed reading message from " + queueName, ex);
    }
  }
}
