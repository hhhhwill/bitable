package com.taikang.feishu.bitable.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lark.oapi.Client;

@Configuration
public class FeishuClientConfig {

    // 从 application.yml 中读取配置
    @Value("${feishu.app-id}")
    private String appId;

    @Value("${feishu.app-secret}")
    private String appSecret;

    @Bean
    public Client feishuClient() {
        return Client.newBuilder(appId, appSecret)
                // .logLevel(LogLevel.DEBUG)
                .build();
    }
}