package com.ocr.business.config;

import com.ocr.common.constants.OcrConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange ocrExchange() {
        return new DirectExchange(OcrConstants.MQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue recognizeQueue() {
        return QueueBuilder.durable(OcrConstants.MQ_QUEUE_RECOGNIZE)
                .withArgument("x-dead-letter-exchange", OcrConstants.MQ_EXCHANGE + ".dlx")
                .build();
    }

    @Bean
    public Queue callbackQueue() {
        return QueueBuilder.durable(OcrConstants.MQ_QUEUE_CALLBACK).build();
    }

    @Bean
    public Binding recognizeBinding() {
        return BindingBuilder.bind(recognizeQueue()).to(ocrExchange()).with(OcrConstants.MQ_ROUTING_KEY_RECOGNIZE);
    }

    @Bean
    public Binding callbackBinding() {
        return BindingBuilder.bind(callbackQueue()).to(ocrExchange()).with(OcrConstants.MQ_ROUTING_KEY_CALLBACK);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
