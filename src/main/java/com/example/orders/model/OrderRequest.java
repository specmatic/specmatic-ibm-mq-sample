package com.example.orders.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record OrderRequest(
    @NotNull @Positive Integer id,
    @NotEmpty List<@Valid OrderItem> orderItems) {}
