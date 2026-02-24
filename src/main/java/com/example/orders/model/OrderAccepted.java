package com.example.orders.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderAccepted(
    @NotNull @Positive Integer id,
    @NotNull OrderStatus status,
    @NotBlank String timestamp) {}
