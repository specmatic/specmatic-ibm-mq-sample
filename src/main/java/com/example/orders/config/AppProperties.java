package com.example.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(MqProperties mq, QueueProperties queues, SmokeTestProperties smokeTest) {

  public record MqProperties(
      String host,
      int port,
      String queueManager,
      String channel,
      String username,
      String password,
      long receiveTimeoutMs) {}

  public record QueueProperties(
      String newOrders,
      String wipOrders,
      String cancelRequests,
      String cancelledOrders,
      String acceptedOrders,
      String outForDeliveryOrders) {}

  public record SmokeTestProperties(boolean enabled) {}
}
