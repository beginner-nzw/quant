package com.quant.job.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.quant")
public class DashboardMetricJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardMetricJobApplication.class, args);
    }
}
