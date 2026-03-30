package com.advprog.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerApplication.class, args);
    }

    // Shared HTTP client used to make outbound requests to the simulator (e.g. GET /api/devices/).
    // Declaring it as a @Bean allows Spring to inject it into any service that needs it.
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
