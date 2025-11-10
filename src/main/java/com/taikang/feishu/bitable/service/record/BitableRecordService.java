package com.taikang.feishu.bitable.service.record; // [修改] 包路径

import com.lark.oapi.Client;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordReq;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordReqBody;
import com.lark.oapi.service.bitable.v1.model.BatchCreateAppTableRecordResp;
import com.lark.oapi.service.bitable.v1.model.AppTableRecord;
import com.taikang.feishu.bitable.exception.BitableApiException; // [修改] 引用
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

    public BatchCreateAppTableRecordResp batchCreateRecords(String appToken, String tableId, List<AppTableRecord> records) throws Exception {

        final int PAGE_SIZE = 500;
        int totalSize = records.size();
        BatchCreateAppTableRecordResp lastResp = null; // 用于返回最后一次的响应

        for (int fromIndex = 0; fromIndex < totalSize; fromIndex += PAGE_SIZE) {
            int toIndex = Math.min(fromIndex + PAGE_SIZE, totalSize);
            List<AppTableRecord> subList = records.subList(fromIndex, toIndex);

            BatchCreateAppTableRecordReqBody reqBody = BatchCreateAppTableRecordReqBody.newBuilder()
                    .records(subList.toArray(new AppTableRecord[0]))
                    .build();

            BatchCreateAppTableRecordReq req = BatchCreateAppTableRecordReq.newBuilder()
                    .appToken(appToken)
                    .tableId(tableId)
                    .batchCreateAppTableRecordReqBody(reqBody)
                    .build();

            log.info("开始向 TableId: {} 批量写入 {} 条记录 ({} - {})", tableId, subList.size(), fromIndex, toIndex - 1);
            BatchCreateAppTableRecordResp resp = client.bitable().appTableRecord().batchCreate(req);
            lastResp = resp; // 保存当前响应

            if (!resp.success()) {
                log.error("批量写入记录失败 ({} - {}), code:{}, msg:{}, reqId:{}",
                        fromIndex, toIndex - 1, resp.getCode(), resp.getMsg(), resp.getRequestId());
                throw new BitableApiException(resp.getCode(), resp.getMsg(), resp.getRequestId());
            }

            log.info("批量写入记录成功 ({} - {})", fromIndex, toIndex - 1);
        }

        log.info("全部 {} 条记录批量写入完成", totalSize);
        return lastResp; // 返回最后一次的 API 响应
    }
}