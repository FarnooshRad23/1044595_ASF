package com.advprog.processing;

import com.advprog.processing.service.SensorCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ProcessingApplicationTests {

    @MockitoBean
    SensorCacheService sensorCacheService;  // prevents @PostConstruct HTTP call

    @Test
    void contextLoads() {
    }

}
