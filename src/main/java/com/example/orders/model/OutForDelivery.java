package com.example.orders.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OutForDelivery(
    @NotNull @Positive Integer orderId,
    @NotBlank String deliveryAddress,
    @NotBlank String deliveryDate) {}
