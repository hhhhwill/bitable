package com.taikang.feishu.bitable.service;

import com.lark.oapi.Client;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordReq;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordReqBody;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordResp;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import com.taikang.feishu.bitable.exception.BitableApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BitableRecordService {

    private static final Logger log = LoggerFactory.getLogger(BitableRecordService.class);

    @Autowired
    private Client client;

    /**
     * 批量向指定数据表写入记录
     *
     * @param appToken 多维表格 App Token
     * @param tableId  数据表 ID
     * @param records  要写入的记录列表 (每条记录是一个 Map)
     * @return 批量创建的结果
     * @throws Exception API 调用异常
     */
    public BatchCreateAppTableRecordResp batchCreateRecords(String appToken, String tableId, List<AppTableRecord> records) throws Exception {
        
        // 1. 飞书 API 批量写入一次最多 500 条, 你可能需要在这里添加分页逻辑
        //    (为保持示例简洁, 这里假设 records.size() <= 500)
        
        // 2. 准备请求体
        BatchCreateAppTableRecordReqBody reqBody = BatchCreateAppTableRecordReqBody.newBuilder()
                .records(records)
                .build();

        // 3. 准备请求
        BatchCreateAppTableRecordReq req = BatchCreateAppTableRecordReq.newBuilder()
                .appToken(appToken)
                .tableId(tableId)
                .batchCreateAppTableRecordReqBody(reqBody)
                .build();

        // 4. 发起请求
        log.info("开始向 TableId: {} 批量写入 {} 条记录", tableId, records.size());
        BatchCreateAppTableRecordResp resp = client.bitable().appTableRecord().batchCreate(req);

        // 5. 处理响应
        if (!resp.success()) {
            log.error("批量写入记录失败, code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
        }

        log.info("批量写入记录成功");
        return resp;
    }
}