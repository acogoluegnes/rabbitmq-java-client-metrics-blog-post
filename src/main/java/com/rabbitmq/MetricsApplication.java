package com.rabbitmq;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.StandardMetricsCollector;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@EnableAutoConfiguration
public class MetricsApplication {

    private final static String METRICS_QUEUE = "metrics.queue";

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext ctx = SpringApplication.run(MetricsApplication.class, args);

        MetricRegistry registry = ctx.getBean(MetricRegistry.class);

        JmxReporter reporter = JmxReporter
            .forRegistry(registry)
            .inDomain("com.rabbitmq.client.jmx")
            .build();
        reporter.start();

        RabbitTemplate template = ctx.getBean(RabbitTemplate.class);
        while(true) {
            Thread.sleep(100L);
            template.convertAndSend(METRICS_QUEUE, "Hello, world!");
        }
    }

    @Bean
    public SimpleMessageListenerContainer messageListenerContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(METRICS_QUEUE);
        container.setMessageListener(exampleListener());
        return container;
    }

    @Bean
    public MessageListener exampleListener() {
        return new MessageListener() {
            public void onMessage(Message message) {
                //System.out.println("received: " + message);
            }
        };
    }

    @Bean
    public Queue metricsQueue() {
        return new Queue(METRICS_QUEUE);
    }

    @Autowired CachingConnectionFactory connectionFactory;

    @Autowired MetricRegistry registry;

    @PostConstruct
    public void init() {
        ConnectionFactory rabbitConnectionFactory = connectionFactory.getRabbitConnectionFactory();
        StandardMetricsCollector metrics = new StandardMetricsCollector(registry);
        rabbitConnectionFactory.setMetricsCollector(metrics);
    }

}
