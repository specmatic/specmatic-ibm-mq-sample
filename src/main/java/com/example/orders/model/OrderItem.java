package com.example.orders.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItem(
    @NotNull @Positive Integer id,
    @NotBlank String name,
    @NotNull @Positive Integer quantity,
    @NotNull @DecimalMin("0.0") Double price) {}
