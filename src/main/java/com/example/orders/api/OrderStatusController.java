package com.example.orders.api;

import com.example.orders.model.OrderItem;
import com.example.orders.model.OrderStatus;
import com.example.orders.service.OrderStateStore;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderStatusController {

  private final OrderStateStore orderStateStore;

  public OrderStatusController(OrderStateStore orderStateStore) {
    this.orderStateStore = orderStateStore;
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderStatusResponse> getOrder(
      @PathVariable Integer orderId,
      @RequestParam(name = "status", required = false) OrderStatus status) {
    return orderStateStore.getState(orderId)
        .filter(state -> status == null || state.status() == status)
        .map(state -> ResponseEntity.ok(new OrderStatusResponse(
            orderId,
            List.of(),
            state.lastUpdatedDate(),
            state.status())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  public record OrderStatusResponse(
      Integer id,
      List<OrderItem> orderItems,
      String lastUpdatedDate,
      OrderStatus status) {}
}
