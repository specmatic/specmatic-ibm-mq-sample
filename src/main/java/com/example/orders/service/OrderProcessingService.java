package com.example.orders.service;

import com.example.orders.config.AppProperties;
import com.example.orders.model.CancellationReference;
import com.example.orders.model.CancelOrderRequest;
import com.example.orders.model.Order;
import com.example.orders.model.OrderAccepted;
import com.example.orders.model.OrderRequest;
import com.example.orders.model.OrderStatus;
import com.example.orders.model.OutForDelivery;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

  private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

  private final JsonService jsonService;
  private final ValidationService validationService;
  private final MqPublisher publisher;
  private final AppProperties appProperties;
  private final OrderStateStore orderStateStore;
  private final AtomicInteger cancellationSequence = new AtomicInteger(1000);

  public OrderProcessingService(
      JsonService jsonService,
      ValidationService validationService,
      MqPublisher publisher,
      AppProperties appProperties,
      OrderStateStore orderStateStore) {
    this.jsonService = jsonService;
    this.validationService = validationService;
    this.publisher = publisher;
    this.appProperties = appProperties;
    this.orderStateStore = orderStateStore;
  }

  public void onNewOrder(String payload, String correlationId) {
    OrderRequest request = validationService.requireValid(jsonService.read(payload, OrderRequest.class));
    double totalAmount = request.orderItems().stream()
        .mapToDouble(item -> item.quantity() * item.price())
        .sum();

    Order order = validationService.requireValid(new Order(request.id(), totalAmount, OrderStatus.INITIATED));
    publisher.publish(appProperties.queues().wipOrders(), jsonService.write(order), correlationId);
    orderStateStore.setStatus(order.id(), order.status());
    log.info("Order {} initiated and published to {}", order.id(), appProperties.queues().wipOrders());
  }

  public void onCancelRequest(String payload, String correlationId) {
    CancelOrderRequest request =
        validationService.requireValid(jsonService.read(payload, CancelOrderRequest.class));

    int referenceValue = cancellationSequence.incrementAndGet();
    CancellationReference response = validationService.requireValid(
        new CancellationReference(referenceValue, OrderStatus.CANCELLED));
    publisher.publish(appProperties.queues().cancelledOrders(), jsonService.write(response), correlationId);
    orderStateStore.setStatus(request.id(), OrderStatus.CANCELLED);
    log.info("Cancellation {} created for order {}", referenceValue, request.id());
  }

  public void onWipOrder(String payload, String correlationId) {
    Order order = validationService.requireValid(jsonService.read(payload, Order.class));

    OrderAccepted accepted = validationService.requireValid(new OrderAccepted(
        order.id(),
        OrderStatus.ACCEPTED,
        Instant.now().toString()));
    publishAccepted(accepted, correlationId, accepted.timestamp());
  }

  public void onOutForDelivery(String payload) {
    OutForDelivery message = validationService.requireValid(jsonService.read(payload, OutForDelivery.class));
    try {
      java.time.LocalDate.parse(message.deliveryDate());
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("deliveryDate must be ISO date (yyyy-MM-dd)", ex);
    }
    orderStateStore.setStatus(message.orderId(), OrderStatus.SHIPPED);
    log.info("Order {} marked SHIPPED for delivery on {}", message.orderId(), message.deliveryDate());
  }

  public void acceptOrder(Integer orderId, String correlationId) {
    acceptOrder(orderId, correlationId, Instant.now().toString());
  }

  public void acceptOrder(Integer orderId, String correlationId, String timestamp) {
    OrderAccepted accepted = validationService.requireValid(new OrderAccepted(
        orderId,
        OrderStatus.ACCEPTED,
        timestamp));
    publishAccepted(accepted, correlationId, timestamp);
  }

  private void publishAccepted(OrderAccepted accepted, String correlationId, String timestamp) {
    publisher.publish(appProperties.queues().acceptedOrders(), jsonService.write(accepted), correlationId);
    orderStateStore.setStatus(accepted.id(), OrderStatus.ACCEPTED, timestamp);
    log.info("Order {} accepted and published to {}", accepted.id(), appProperties.queues().acceptedOrders());
  }
}
