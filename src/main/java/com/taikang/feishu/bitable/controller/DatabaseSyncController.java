package com.taikang.feishu.bitable.controller;

import com.taikang.feishu.bitable.service.DatabaseSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 用于触发数据同步任务的 Controller
 */
@RestController
@RequestMapping("/api/sync") // 定义此 Controller 的基础 URL
public class DatabaseSyncController {

    @Autowired
    private DatabaseSyncService syncService;

    /**
     * 触发一个从数据库到飞书多维表格的同步任务
     *
     * @param appToken          飞书 Bitable App Token
     * @param dbTableName       要同步的数据库表名
     * @param newBitableTableName 在飞书中创建的新表名
     * @return 包含 tableId 或 错误信息 的 JSON 响应
     */
    @PostMapping("/table") // 使用 POST, 因为这是一个会创建数据的操作
    public ResponseEntity<Map<String, String>> syncDatabaseTable(
            @RequestParam String appToken,
            @RequestParam String dbTableName,
            @RequestParam String newBitableTableName) {
        
        try {
            // 1. 调用核心服务执行同步
            String newTableId = syncService.syncTableToBitable(appToken, dbTableName, newBitableTableName);
            
            // 2. 同步成功，返回 200 OK 和新创建的 tableId
            Map<String, String> response = Collections.singletonMap("tableId", newTableId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // 3. 出现异常，返回 500 Internal Server Error 和错误信息
            // 在生产环境中，不应暴露详细的 e.getMessage()，这里为了调试方便
            e.printStackTrace(); // 打印堆栈信息
            Map<String, String> errorResponse = Collections.singletonMap("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}