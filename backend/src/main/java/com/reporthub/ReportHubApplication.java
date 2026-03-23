package com.reporthub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.reporthub.mapper")
public class ReportHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportHubApplication.class, args);
    }
}
