package com.taikang.feishu.bitable.service.app;

import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.service.bitable.v1.model.*;
import com.taikang.feishu.bitable.exception.BitableApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BitableAppService {

    private static final Logger log = LoggerFactory.getLogger(BitableAppService.class);

    @Autowired
    private Client client;

    public App createBitableApp(String documentName, String folderToken, String userAccessToken) throws Exception {
        ReqApp reqApp = ReqApp.newBuilder()
                .name(documentName)
                .folderToken(folderToken)
                .build();

        CreateAppReq req = CreateAppReq.newBuilder()
                .reqApp(reqApp)
                .build();

        log.info("开始创建新的多维表格文档, 名称: {}", documentName);

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