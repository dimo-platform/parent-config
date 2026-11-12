package com.dimo.parent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class ErrorMessageConfig {

    @Value("${error.messages.basenames:parent-errors}")
    private String[] basenames;

    @Bean
    public MessageSource errorMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

        messageSource.setBasenames(basenames);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);

        return messageSource;
    }

}
