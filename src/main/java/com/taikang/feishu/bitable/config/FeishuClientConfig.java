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

    /**
     * 将官方 Client 注册为 Spring Bean
     * 官方 SDK 内部会自动管理 tenant_access_token 的获取和刷新
     */
    @Bean
    public Client feishuClient() {
        return Client.newBuilder(appId, appSecret)
                // .logLevel(LogLevel.DEBUG) // 开发时建议打开日志，便于排查问题
                .build();
    }
}