package com.example.orders.config;

import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.wmq.WMQConstants;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.DestinationResolver;

@Configuration
@EnableJms
public class JmsConfig {

  private static final String MQ_QUEUE_PREFIX = "queue:///";

  @Bean
  public ConnectionFactory connectionFactory(AppProperties properties) throws JMSException {
    AppProperties.MqProperties mq = properties.mq();

    MQConnectionFactory mqConnectionFactory = new MQConnectionFactory();
    mqConnectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
    mqConnectionFactory.setHostName(mq.host());
    mqConnectionFactory.setPort(mq.port());
    mqConnectionFactory.setQueueManager(mq.queueManager());
    mqConnectionFactory.setChannel(mq.channel());
    mqConnectionFactory.setStringProperty(WMQConstants.USERID, mq.username());
    mqConnectionFactory.setStringProperty(WMQConstants.PASSWORD, mq.password());
    mqConnectionFactory.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);

    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(mqConnectionFactory);
    cachingConnectionFactory.setSessionCacheSize(8);
    return cachingConnectionFactory;
  }

  @Bean
  public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
      ConnectionFactory connectionFactory,
      DefaultJmsListenerContainerFactoryConfigurer configurer,
      DestinationResolver destinationResolver) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    factory.setDestinationResolver(destinationResolver);
    factory.setSessionTransacted(false);
    return factory;
  }

  @Bean
  public DestinationResolver destinationResolver() {
    return (Session session, String destinationName, boolean pubSubDomain) -> {
      if (pubSubDomain) {
        return session.createTopic(destinationName);
      }
      return session.createQueue(toMqQueueName(destinationName));
    };
  }

  @Bean
  public JmsTemplate jmsTemplate(
      ConnectionFactory connectionFactory,
      AppProperties properties,
      DestinationResolver destinationResolver) {
    JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
    jmsTemplate.setDestinationResolver(destinationResolver);
    jmsTemplate.setReceiveTimeout(properties.mq().receiveTimeoutMs());
    return jmsTemplate;
  }

  private static String toMqQueueName(String destinationName) {
    if (destinationName == null || destinationName.isBlank()) {
      return destinationName;
    }
    if (destinationName.startsWith("queue:///") || destinationName.startsWith("topic:///")) {
      return destinationName;
    }
    return MQ_QUEUE_PREFIX + destinationName;
  }
}
