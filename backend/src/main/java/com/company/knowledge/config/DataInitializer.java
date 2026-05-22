package com.company.knowledge.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedBaseData() {
        return args -> {
            // 测试数据初始化已禁用
        };
    }
}
