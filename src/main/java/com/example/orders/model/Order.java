package com.example.orders.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Order(
    @NotNull @Positive Integer id,
    @NotNull @DecimalMin("0.0") Double totalAmount,
    @NotNull OrderStatus status) {}
