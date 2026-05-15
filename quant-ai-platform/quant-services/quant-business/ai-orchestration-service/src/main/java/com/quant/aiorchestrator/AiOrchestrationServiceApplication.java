package com.quant.aiorchestrator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.quant.aiorchestrator.mapper")
@SpringBootApplication(scanBasePackages = "com.quant")
public class AiOrchestrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiOrchestrationServiceApplication.class, args);
    }

}
