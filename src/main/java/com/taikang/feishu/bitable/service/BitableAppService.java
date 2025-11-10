package com.taikang.feishu.bitable.service;

import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.service.bitable.v1.model.CreateAppReq;
import com.lark.oapi.service.bitable.v1.model.CreateAppResp;
import com.lark.oapi.service.bitable.v1.model.ReqApp;
import com.lark.oapi.service.bitable.v1.model.App;
import com.taikang.feishu.bitable.exception.BitableApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 封装用于“创建多维表格文档”的 Service
 */
@Service
public class BitableAppService {

    private static final Logger log = LoggerFactory.getLogger(BitableAppService.class);

    @Autowired
    private Client client; // 自动注入已配置好的 Client

    /**
     * 创建一个新的多维表格文档
     *
     * @param documentName    要创建的文档名称
     * @param folderToken     (可选) 文件夹 Token
     * @param userAccessToken (可选) 如果提供, 则以此用户身份创建; 否则, 以应用身份(tenant)创建
     * @return 完整的 App 对象, 包含 appToken, url 等
     * @throws Exception API 调用异常
     */
    public App createBitableApp(String documentName, String folderToken, String userAccessToken) throws Exception {

        // 1. 准备请求体
        ReqApp reqApp = ReqApp.newBuilder()
                .name(documentName)
                .folderToken(folderToken)
                .build();

        CreateAppReq req = CreateAppReq.newBuilder()
                .reqApp(reqApp)
                .build();

        log.info("开始创建新的多维表格文档, 名称: {}", documentName);

        // 2. 发起请求 (根据 userAccessToken 是否存在, 决定调用方式)
        CreateAppResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            log.info("使用 User Access Token 发起请求");
            resp = client.bitable().v1().app().create(req, RequestOptions.newBuilder()
                    .userAccessToken(userAccessToken)
                    .build());
        } else {
            log.info("使用 Tenant Access Token (默认) 发起请求");
            resp = client.bitable().v1().app().create(req);
        }

        // 3. 处理响应
        if (!resp.success()) {
            log.error("创建多维表格文档失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        App app = resp.getData().getApp();
        log.info("创建多维表格文档成功, appToken: {}, url: {}", app.getAppToken(), app.getUrl());

        return app;
    }
}