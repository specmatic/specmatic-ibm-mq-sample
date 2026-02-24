package com.example.orders.api;

import com.example.orders.model.OrderAccepted;
import com.example.orders.model.OrderStatus;
import com.example.orders.service.OrderProcessingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderAcceptanceController {

  private static final String ORDER_CORRELATION_ID = "orderCorrelationId";

  private final OrderProcessingService orderProcessingService;

  public OrderAcceptanceController(OrderProcessingService orderProcessingService) {
    this.orderProcessingService = orderProcessingService;
  }

  @PutMapping("/{orderId}/accept")
  public ResponseEntity<Void> acceptOrder(
      @PathVariable Integer orderId,
      @RequestHeader(name = ORDER_CORRELATION_ID, required = false) String correlationId) {
    orderProcessingService.acceptOrder(orderId, correlationId);
    return ResponseEntity.accepted().build();
  }

  @PutMapping
  public ResponseEntity<Void> acceptOrder(
      @Valid @RequestBody OrderAccepted orderAccepted,
      @RequestHeader(name = ORDER_CORRELATION_ID, required = false) String correlationId) {
    if (orderAccepted.status() != OrderStatus.ACCEPTED) {
      return ResponseEntity.badRequest().build();
    }
    orderProcessingService.acceptOrder(orderAccepted.id(), correlationId, orderAccepted.timestamp());
    return ResponseEntity.ok().build();
  }
}
