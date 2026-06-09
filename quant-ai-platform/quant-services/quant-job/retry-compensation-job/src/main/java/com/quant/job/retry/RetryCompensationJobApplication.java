package com.quant.job.retry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.quant")
public class RetryCompensationJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetryCompensationJobApplication.class, args);
    }
}
