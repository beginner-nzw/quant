package com.quant.report;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.quant.aiorchestrator.mapper")
public class ReportServiceMapperScanConfig {
}
