package com.clawreport;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.clawreport.mapper")
public class ClawReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClawReportApplication.class, args);
    }
}
