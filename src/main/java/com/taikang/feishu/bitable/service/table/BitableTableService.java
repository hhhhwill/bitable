package com.taikang.feishu.bitable.service.table;

import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.service.bitable.v1.model.*;
import com.taikang.feishu.bitable.exception.BitableApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import com.google.gson.Gson;

@Service
public class BitableTableService {
    private static final Logger log = LoggerFactory.getLogger(BitableTableService.class);

    @Autowired
    private Client client;

    private static final Gson gson = new Gson();

    /**
     * 【新方法】: 创建数据表
     *
     * @param appToken    App Token
     * @param rawReqBody 飞书 SDK 的 ReqTable 对象, 包含所有自定义配置
     * @return 响应体
     * @throws Exception
     */
    public CreateAppTableRespBody createTable(String appToken, Map<String, Object> rawReqBody) throws Exception {

        // 使用 GSON 将 Map -> JSON 字符串 -> 飞书 SDK 对象
        String jsonString = gson.toJson(rawReqBody);
        CreateAppTableReqBody reqBody = gson.fromJson(jsonString, CreateAppTableReqBody.class);

        CreateAppTableReq req = CreateAppTableReq.newBuilder()
                .appToken(appToken)
                .createAppTableReqBody(reqBody)
                .build();

        log.info("开始创建数据表 (V2 - GSON 解析), appToken: {}, tableName: {}", appToken, reqBody.getTable().getName());

        CreateAppTableResp resp = client.bitable().appTable().create(req);

        if (!resp.success()) {
            log.error("创建数据表 (V2) 失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("创建数据表 (V2) 成功, tableId: {}", resp.getData().getTableId());
        return resp.getData();
    }

    public CreateAppTableRespBody createTableWithFields(String appToken, String tableName, List<AppTableField> fields) throws Exception {

        List<AppTableCreateHeader> headers = new ArrayList<>();
        for (AppTableField field : fields) {
            headers.add(AppTableCreateHeader.newBuilder()
                    .fieldName(field.getFieldName())
                    .type(field.getType())
                    .build());
        }

        ReqTable tableConfig = ReqTable.newBuilder()
                .name(tableName)
                .fields(headers.toArray(new AppTableCreateHeader[0]))
                .build();

        CreateAppTableReqBody reqBody = CreateAppTableReqBody.newBuilder()
                .table(tableConfig)
                .build();

        CreateAppTableReq req = CreateAppTableReq.newBuilder()
                .appToken(appToken)
                .createAppTableReqBody(reqBody)
                .build();

        log.info("开始创建数据表 (含自定义字段), appToken: {}, tableName: {}", appToken, tableName);

        CreateAppTableResp resp = client.bitable().appTable().create(req);

        if (!resp.success()) {
            log.error("创建数据表失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("创建数据表成功, tableId: {}", resp.getData().getTableId());
        return resp.getData();
    }

    public ListAppTableRespBody listTables(String appToken, String userAccessToken, int pageSize) throws Exception {
        ListAppTableReq req = ListAppTableReq.newBuilder()
                .appToken(appToken)
                .pageSize(pageSize)
                .build();

        log.info("开始列出 AppToken: {} 下的所有数据表", appToken);

        ListAppTableResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            resp = client.bitable().v1().appTable().list(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
        } else {
            resp = client.bitable().v1().appTable().list(req);
        }

        if (!resp.success()) {
            log.error("列出数据表失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("列出数据表成功, 共 {} 张表", resp.getData().getTotal());
        return resp.getData();
    }



    public void updateTable(String appToken, String tableId, String newName, String userAccessToken) throws Exception {
        PatchAppTableReq req = PatchAppTableReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .patchAppTableReqBody(PatchAppTableReqBody.newBuilder()
                        .name(newName)
                        .build())
                .build();

        log.info("准备更新数据表, appToken: {}, tableId: {}, newName: {}", appToken, tableId, newName);

        PatchAppTableResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            resp = client.bitable().v1().appTable().patch(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
        } else {
            resp = client.bitable().v1().appTable().patch(req);
        }

        if (!resp.success()) {
            log.error("更新数据表失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("更新数据表成功, tableId: {}", tableId);
    }

    public void deleteTable(String appToken, String tableId, String userAccessToken) throws Exception {
        DeleteAppTableReq req = DeleteAppTableReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .build();

        log.info("准备删除数据表, appToken: {}, tableId: {}", appToken, tableId);

        DeleteAppTableResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            resp = client.bitable().v1().appTable().delete(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
        } else {
            resp = client.bitable().v1().appTable().delete(req);
        }

        if (!resp.success()) {
            log.error("删除数据表失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("删除数据表成功, tableId: {}", tableId);
    }

    public BatchDeleteAppTableResp batchDeleteTables(String appToken, List<String> tableIds, String userAccessToken) throws Exception {
        BatchDeleteAppTableReq req = BatchDeleteAppTableReq.newBuilder()
                .appToken(appToken)
                .batchDeleteAppTableReqBody(BatchDeleteAppTableReqBody.newBuilder()
                        .tableIds(tableIds.toArray(new String[0]))
                        .build())
                .build();

        log.info("准备批量删除数据表, appToken: {}, tableIds: {}", appToken, tableIds);

        BatchDeleteAppTableResp resp;
        if (userAccessToken != null && !userAccessToken.isEmpty()) {
            resp = client.bitable().v1().appTable().batchDelete(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
        } else {
            resp = client.bitable().v1().appTable().batchDelete(req);
        }

        if (!resp.success()) {
            log.error("批量删除数据表失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("批量删除数据表成功");
        return resp;
    }

}