package com.notehub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * notehub 后端入口
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@MapperScan("com.notehub.mapper")
public class NotehubApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotehubApplication.class, args);
    }
}
