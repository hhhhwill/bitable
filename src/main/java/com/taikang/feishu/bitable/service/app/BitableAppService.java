package com.taikang.feishu.bitable.service.app;

import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.service.bitable.v1.model.*;
import com.taikang.feishu.bitable.exception.BitableApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate; // 1. 导入
import org.springframework.stereotype.Service;


@Service
public class BitableAppService {

    private static final Logger log = LoggerFactory.getLogger(BitableAppService.class);

    @Autowired
    private Client client;

    @Autowired
    private JdbcTemplate jdbcTemplate; // 2. 注入 JdbcTemplate

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

        try {
            String sql = "INSERT INTO bitable_apps (app_token, app_name, app_url) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, app.getAppToken(), app.getName(), app.getUrl());
            log.info("新 App 已登记到本地 bitable_apps: {}", app.getAppToken());
        } catch (Exception e_db) {
            // 记录日志，但不抛出异常，确保主流程(创建App)的成功响应
            log.error("飞书 App 创建成功，但写入本地 bitable_apps 表失败: {}", e_db.getMessage(), e_db);
        }

        return app;
    }

    /**
     *
     * @param appToken        要复制的 appToken
     * @param newName         【必需】副本的新名称
     * @param folderToken     (可选) 副本存放的文件夹 token
     * @param withoutContent  (可选) 是否不复制内容, 默认为 false
     * @param userAccessToken (可选) 用户 token
     * @return 复制后得到的新 App 对象
     */
    public App copyApp(String appToken, String newName, String folderToken, boolean withoutContent, String userAccessToken) throws Exception {
        CopyAppReqBody body = CopyAppReqBody.newBuilder()
                .name(newName)
                .folderToken(folderToken)
                .withoutContent(withoutContent)
                .build();

        CopyAppReq req = CopyAppReq.newBuilder()
                .appToken(appToken)
                .copyAppReqBody(body)
                .build();

        log.info("开始复制多维表格, appToken: {}, newName: {}", appToken, newName);

        CopyAppResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            resp = client.bitable().v1().app().copy(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
        } else {
            resp = client.bitable().v1().app().copy(req);
        }

        if (!resp.success()) {
            log.error("复制多维表格失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        App copiedApp = resp.getData().getApp();
        log.info("复制多维表格成功, new AppToken: {}", copiedApp.getAppToken());

        // --- 4. 解决方案：将复制的新 App 登记到本地数据库 ---
        try {
            String sql = "INSERT INTO bitable_apps (app_token, app_name, app_url) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, copiedApp.getAppToken(), copiedApp.getName(), copiedApp.getUrl());
            log.info("复制的新 App 已登记到本地 bitable_apps: {}", copiedApp.getAppToken());
        } catch (Exception e_db) {
            log.error("飞书 App 复制成功，但写入本地 bitable_apps 表失败: {}", e_db.getMessage(), e_db);
        }
        // --- 解决方案结束 ---

        return copiedApp;
    }

    /**
     *
     * @param appToken        要获取元数据的 appToken
     * @param userAccessToken (可选) 用户 token
     * @return App 对象
     */
    public DisplayApp getAppMetadata(String appToken, String userAccessToken) throws Exception {
        GetAppReq req = GetAppReq.newBuilder()
                .appToken(appToken)
                .build();

        log.info("开始获取多维表格元数据, appToken: {}", appToken);

        GetAppResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            resp = client.bitable().v1().app().get(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
        } else {
            resp = client.bitable().v1().app().get(req);
        }

        if (!resp.success()) {
            log.error("获取元数据失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("获取元数据成功, appToken: {}", appToken);
        return resp.getData().getApp();
    }

    /**
     *
     * @param appToken        要更新的 appToken
     * @param newName         (可选) 新的文档名称
     * @param userAccessToken (可选) 用户 token
     */
    public void updateAppMetadata(String appToken, String newName, String userAccessToken) throws Exception {
        UpdateAppReqBody.Builder bodyBuilder = UpdateAppReqBody.newBuilder();
        if (newName != null && !newName.isEmpty()) {
            bodyBuilder.name(newName);
        }
        // 如果 body 为空 (没有提供 newName)，则不执行任何操作
        if (newName == null || newName.isEmpty()) {
            log.warn("updateAppMetadata 调用，但未提供 newName, appToken: {}", appToken);
            return;
        }

        UpdateAppReq req = UpdateAppReq.newBuilder()
                .appToken(appToken)
                .updateAppReqBody(bodyBuilder.build())
                .build();

        log.info("开始更新多维表格元数据, appToken: {}", appToken);

        UpdateAppResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            resp = client.bitable().v1().app().update(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
        } else {
            resp = client.bitable().v1().app().update(req);
        }

        if (!resp.success()) {
            log.error("更新元数据失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("更新元数据成功, appToken: {}", appToken);

        // --- 5. (可选) 解决方案：更新本地数据库中的名称 ---
        try {
            String sql = "UPDATE bitable_apps SET app_name = ? WHERE app_token = ?";
            int rowsAffected = jdbcTemplate.update(sql, newName, appToken);
            log.info("本地 bitable_apps 表中的名称已更新, appToken: {}, 影响行数: {}", appToken, rowsAffected);
        } catch (Exception e_db) {
            log.error("!!! 严重：飞书 App 元数据更新成功，但更新本地 bitable_apps 表失败: {}", e_db.getMessage(), e_db);
        }
        // --- 解决方案结束 ---
    }
}