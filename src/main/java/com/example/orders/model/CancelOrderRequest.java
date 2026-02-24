package com.example.orders.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CancelOrderRequest(@NotNull @Positive Integer id) {}
