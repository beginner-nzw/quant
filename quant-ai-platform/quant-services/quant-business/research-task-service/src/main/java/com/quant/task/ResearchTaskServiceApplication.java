package com.quant.task;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.quant.task.mapper")
@SpringBootApplication(scanBasePackages = "com.quant")
public class ResearchTaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResearchTaskServiceApplication.class, args);
    }

}
