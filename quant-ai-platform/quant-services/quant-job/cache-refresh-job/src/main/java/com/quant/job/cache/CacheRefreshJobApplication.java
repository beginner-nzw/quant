package com.quant.job.cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.quant")
public class CacheRefreshJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(CacheRefreshJobApplication.class, args);
    }
}
