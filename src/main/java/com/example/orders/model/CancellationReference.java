package com.example.orders.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CancellationReference(
    @NotNull @Positive Integer reference,
    @NotNull OrderStatus status) {}
