package com.dimo.parent.config;

import com.dimo.parent.logging.LoggingAspect;
import com.dimo.parent.logging.definition.LoggingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = {"com.dimo.parent"})
public class GlobalConfig {

    @Bean
    public LoggingProperties loggingProperties(
            @Value("${spring.application.name:parent-config}") String name,
            @Value("${logging.level.com.dimo:debug}") String logLevel) {
        return new LoggingProperties(name, logLevel);
    }

    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

}
