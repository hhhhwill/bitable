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

    // 在 BitableRecordService.java 中
    public void batchCreateRecords(String appToken, String tableId, List<AppTableRecord> records) throws Exception {

        final int PAGE_SIZE = 500;
        int totalSize = records.size();

        for (int fromIndex = 0; fromIndex < totalSize; fromIndex += PAGE_SIZE) {
            int toIndex = Math.min(fromIndex + PAGE_SIZE, totalSize);
            List<AppTableRecord> subList = records.subList(fromIndex, toIndex);

            // --- 准备请求体 (使用子列表) ---
            BatchCreateAppTableRecordReqBody reqBody = BatchCreateAppTableRecordReqBody.newBuilder()
                    .records(subList.toArray(new AppTableRecord[0])) // 同样进行修正
                    .build();

            BatchCreateAppTableRecordReq req = BatchCreateAppTableRecordReq.newBuilder()
                    .appToken(appToken)
                    .tableId(tableId)
                    .batchCreateAppTableRecordReqBody(reqBody)
                    .build();

            // --- 发起分页请求 ---
            log.info("开始向 TableId: {} 批量写入 {} 条记录 ({} - {})", tableId, subList.size(), fromIndex, toIndex - 1);
            BatchCreateAppTableRecordResp resp = client.bitable().appTableRecord().batchCreate(req);

            if (!resp.success()) {
                log.error("批量写入记录失败 ({} - {}), code:{}, msg:{}, reqId:{}",
                        fromIndex, toIndex - 1, resp.getCode(), resp.getMsg(), resp.getRequestId());
                // 抛出异常，中断后续分页
                throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
            }

            log.info("批量写入记录成功 ({} - {})", fromIndex, toIndex - 1);

            // (可选) 增加短暂休眠，防止API频率超限
            // Thread.sleep(500);
        }

        log.info("全部 {} 条记录批量写入完成", totalSize);
        // 注意：此方法现在可以改为 void，或者返回最后一个 Resp
    }
}