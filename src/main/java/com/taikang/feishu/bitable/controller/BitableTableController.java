package com.taikang.feishu.bitable.controller;

import com.taikang.feishu.bitable.service.sync.DatabaseSyncService; // 假设 DatabaseSyncService 也会被移动
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/bitable/table")
public class BitableTableController {


    @Autowired
    private DatabaseSyncService syncService;

    /**
     * 【API 2: 从数据库同步数据来创建数据表 (Table)】
     * 调用此 API, 将本地数据库表同步到【已存在】的多维表格文档中, 创建一张新【数据表】。
     *
     * @param appToken          【必需】飞书 Bitable App Token (bascn...)
     * @param dbTableName       要同步的数据库表名 (例如 "employees")
     * @param newBitableTableName 在飞书中创建的新【数据表】名
     * @return 包含 tableId 的 JSON 响应
     */
    @PostMapping("/sync-from-db")
    public ResponseEntity<Map<String, Object>> syncTableFromDatabase(
            @RequestParam String appToken,
            @RequestParam String dbTableName,
            @RequestParam String newBitableTableName) {

        try {
            // 1. 调用核心服务执行同步
            String newTableId = syncService.syncTableToBitable(appToken, dbTableName, newBitableTableName);

            // 2. 同步成功
            Map<String, Object> response = new HashMap<>();
            response.put("message", "数据表同步创建成功");
            response.put("appToken", appToken);
            response.put("newTableId", newTableId);

            String link = String.format("https://feishu.cn/base/%s?table=%s", appToken, newTableId);
            response.put("quickAccessLink", link);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 3. 出现异常
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // TODO: 在这里添加您未来的 "创建空表"、"删除表"、"获取表元数据" 等 API

}