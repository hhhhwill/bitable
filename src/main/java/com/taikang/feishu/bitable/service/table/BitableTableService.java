package com.taikang.feishu.bitable.service.table;

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

    // TODO: 在这里添加 "listTables", "deleteTable" 等 Service 方法
}