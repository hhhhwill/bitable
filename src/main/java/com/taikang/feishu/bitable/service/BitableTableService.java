package com.taikang.feishu.bitable.service;

import com.lark.oapi.Client;
import com.lark.oapi.service.bitable.v1.model.*;
import com.taikang.feishu.bitable.exception.BitableApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BitableTableService {

    private static final Logger log = LoggerFactory.getLogger(BitableTableService.class);

    @Autowired
    private Client client;

    /**
     * 在指定 App 中创建一张新的数据表, 并自定义字段
     *
     * @param appToken    多维表格 App Token
     * @param tableName   新数据表的名称
     * @param fields      要创建的字段列表 (类型为 AppTableField)
     * @return 新创建的数据表的信息
     * @throws Exception API 调用异常
     */
    public CreateAppTableRespBody createTableWithFields(String appToken, String tableName, List<AppTableField> fields) throws Exception {

        List<AppTableCreateHeader> headers = new ArrayList<>();
        for (AppTableField field : fields) {
            headers.add(AppTableCreateHeader.newBuilder()
                    .fieldName(field.getFieldName())
                    .type(field.getType())
                    .build());
        }

        // 1. 准备 ReqTable 对象
        // ReqTable.Builder 自身就包含了 .fields() 方法
        ReqTable tableConfig = ReqTable.newBuilder()
                .name(tableName)
                .fields(headers.toArray(new AppTableCreateHeader[0]))
                .build();

        // 2. 准备 CreateAppTableReqBody
        // .table() 方法需要一个 ReqTable 对象
        CreateAppTableReqBody reqBody = CreateAppTableReqBody.newBuilder()
                .table(tableConfig)
                .build();

        // 3. 准备完整的请求
        CreateAppTableReq req = CreateAppTableReq.newBuilder()
                .appToken(appToken)
                .createAppTableReqBody(reqBody)
                .build();

        log.info("开始创建数据表 (含自定义字段), appToken: {}, tableName: {}", appToken, tableName);

        // 4. 发起 API 调用
        CreateAppTableResp resp = client.bitable().appTable().create(req);

        // 5. 处理响应
        if (!resp.success()) {
            log.error("创建数据表失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("创建数据表成功, tableId: {}", resp.getData().getTableId());
        return resp.getData();
    }
}