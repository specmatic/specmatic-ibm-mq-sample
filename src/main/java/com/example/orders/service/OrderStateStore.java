package com.example.orders.service;

import com.example.orders.model.OrderStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OrderStateStore {

  public record OrderState(OrderStatus status, String lastUpdatedDate) {}

  private final Map<Integer, OrderState> states = new ConcurrentHashMap<>();

  public void setStatus(int orderId, OrderStatus status) {
    setStatus(orderId, status, Instant.now().toString());
  }

  public void setStatus(int orderId, OrderStatus status, String lastUpdatedDate) {
    states.put(orderId, new OrderState(status, lastUpdatedDate));
  }

  public Optional<OrderState> getState(int orderId) {
    return Optional.ofNullable(states.get(orderId));
  }

  public Optional<OrderStatus> getStatus(int orderId) {
    return getState(orderId).map(OrderState::status);
  }
}
