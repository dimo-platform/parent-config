package com.dimo.parent.client.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@AutoConfiguration
@EnableFeignClients(basePackages = {
        "com.dimo.parent.client"
})
public class CommonClientsAutoConfiguration {
}
