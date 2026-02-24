package com.example.orders.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {

  private final Validator validator;

  public ValidationService(Validator validator) {
    this.validator = validator;
  }

  public <T> T requireValid(T value) {
    Set<ConstraintViolation<T>> violations = validator.validate(value);
    if (!violations.isEmpty()) {
      String details = violations.stream()
          .map(v -> v.getPropertyPath() + " " + v.getMessage())
          .sorted()
          .collect(Collectors.joining("; "));
      throw new IllegalArgumentException("Validation failed: " + details);
    }
    return value;
  }
}
