package com.example.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.orders.config.AppProperties;
import com.example.orders.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrderProcessingServiceTest {

  private MqPublisher publisher;
  private OrderStateStore stateStore;
  private OrderProcessingService service;

  @BeforeEach
  void setUp() {
    publisher = Mockito.mock(MqPublisher.class);
    stateStore = new OrderStateStore();

    AppProperties properties = new AppProperties(
        new AppProperties.MqProperties("localhost", 1415, "QM1", "DEV.APP.SVRCONN", "app", "pass", 5000),
        new AppProperties.QueueProperties(
            "new-orders",
            "wip-orders",
            "to-be-cancelled-orders",
            "cancelled-orders",
            "accepted-orders",
            "out-for-delivery-orders"),
        new AppProperties.SmokeTestProperties(false));

    service = new OrderProcessingService(
        new JsonService(new ObjectMapper()),
        new ValidationService(Validation.buildDefaultValidatorFactory().getValidator()),
        publisher,
        properties,
        stateStore);
  }

  @Test
  void placeOrderPublishesInitiatedOrderToWipQueue() {
    String payload = """
        {
          "id": 10,
          "orderItems": [
            { "id": 1, "name": "Book", "quantity": 2, "price": 12.5 },
            { "id": 2, "name": "Pen", "quantity": 3, "price": 1.0 }
          ]
        }
        """;

    service.onNewOrder(payload, "corr-1");

    verify(publisher).publish(eq("wip-orders"), any(String.class), eq("corr-1"));
    assertThat(stateStore.getStatus(10)).contains(OrderStatus.INITIATED);
  }

  @Test
  void cancelOrderPublishesCancellationReference() {
    service.onCancelRequest("{\"id\":10}", "corr-2");

    verify(publisher).publish(eq("cancelled-orders"), any(String.class), eq("corr-2"));
    assertThat(stateStore.getStatus(10)).contains(OrderStatus.CANCELLED);
  }
}
