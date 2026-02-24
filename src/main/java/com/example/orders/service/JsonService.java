package com.example.orders.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class JsonService {

  private final ObjectMapper objectMapper;

  public JsonService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public <T> T read(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Invalid JSON for " + type.getSimpleName(), ex);
    }
  }

  public String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize " + value.getClass().getSimpleName(), ex);
    }
  }
}
