package com.taikang.feishu.bitable.controller;

import com.lark.oapi.service.bitable.v1.model.*;
import com.taikang.feishu.bitable.exception.BitableApiException;
import com.taikang.feishu.bitable.service.sync.DatabaseSyncService;
import com.taikang.feishu.bitable.service.table.BitableTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/bitable/table")
public class BitableTableController {

    private static final Logger log = LoggerFactory.getLogger(BitableTableController.class);

    @Autowired
    private DatabaseSyncService syncService;

    @Autowired
    private BitableTableService tableService;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * 【API 1: 手动创建数据表 (Table)】
     * 调用此 API, 在【已存在】的多维表格文档中, 根据飞书 SDK (ReqTable) 结构创建一张新【数据表】。
     *
     * @param appToken    【必需】飞书 Bitable App Token (bascn...)
     * @param rawReqBody 【必需】请求体 (JSON)
     * @return 包含 tableId 的 JSON 响应
     */
    @PostMapping("/create/{appToken}")
    public ResponseEntity<Map<String, Object>> createTable(
            @PathVariable String appToken,
            @RequestBody Map<String, Object> rawReqBody) {

        String tableName = "Unknown";
        try {
            if (!rawReqBody.containsKey("table") || !(rawReqBody.get("table") instanceof Map)) {
                throw new IllegalArgumentException("请求体中必须包含 'table' 键, 且其值必须是一个 JSON 对象");
            }
            Map<String, Object> tableMap = (Map<String, Object>) rawReqBody.get("table");

            if (!tableMap.containsKey("name") || !(tableMap.get("name") instanceof String) || ((String)tableMap.get("name")).isEmpty()) {
                throw new IllegalArgumentException("请求体中 'table.name' 字段 (即 tableName) 不能为空");
            }
            tableName = (String) tableMap.get("name");
            log.info("name字段：{}", tableName);

            // 5. 将整个 Map 传递给 Service
            CreateAppTableRespBody respBody = tableService.createTable(
                    appToken,
                    rawReqBody
            );
            String newTableId = respBody.getTableId();

            // 6. 登记到数据库
            jdbcTemplate.update("INSERT INTO bitable_tables (table_id, app_token, table_name, source_db_table_name) " +
                            "VALUES (?, ?, ?, ?)",
                    newTableId, appToken, tableName, null);
            log.info("手动创建的数据表已登记到 bitable_tables: {}", newTableId);

            // 7. 构造成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("message", "数据表创建成功");
            response.put("appToken", appToken);
            response.put("newTableId", newTableId);
            String link = String.format("https://feishu.cn/base/%s?table=%s", appToken, newTableId);
            response.put("quickAccessLink", link);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("创建数据表失败 (Controller), tableName: {}", tableName, e);
            return buildErrorResponse(e);
        }
    }

    /**
     * 【API 3: (模拟) 批量创建数据表 - 增强版】
     *
     * @param appToken     【必需】飞书 Bitable App Token (bascn...)
     * @param rawReqBodies 【必需】请求体 (JSON), 包含 ReqTable 的【列表】
     * @return 包含成功和失败信息的响应
     */
    @PostMapping("/batch-create/{appToken}")
    public ResponseEntity<Map<String, Object>> batchCreateTables(
            @PathVariable String appToken,
            @RequestBody List<Map<String, Object>> rawReqBodies) { // <-- 【重要】使用 ReqTable 列表

        List<Map<String, Object>> successes = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();

        for (Map<String, Object> rawReqBody : rawReqBodies) {
            String tableName = "Unknown";
            try {
                // 同样的手动校验
                if (!rawReqBody.containsKey("table") || !(rawReqBody.get("table") instanceof Map)) {
                    throw new IllegalArgumentException("请求体中必须包含 'table' 键");
                }
                Map<String, Object> tableMap = (Map<String, Object>) rawReqBody.get("table");
                if (!tableMap.containsKey("name") || !(tableMap.get("name") instanceof String)) {
                    throw new IllegalArgumentException("'table.name' 不能为空");
                }
                tableName = (String) tableMap.get("name");

                // 调用 Service
                CreateAppTableRespBody respBody = tableService.createTable(
                        appToken,
                        rawReqBody
                );
                String newTableId = respBody.getTableId();

                // 登记数据库
                jdbcTemplate.update("INSERT INTO bitable_tables ... (同上)",
                        newTableId, appToken, tableName, null);

                Map<String, Object> successDetail = new HashMap<>();
                successDetail.put("tableName", tableName);
                successDetail.put("newTableId", newTableId);
                successes.add(successDetail);

            } catch (Exception e) {
                Map<String, Object> failureDetail = new HashMap<>();
                failureDetail.put("tableName", tableName);
                failureDetail.put("error", e.getMessage());
                failures.add(failureDetail);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", String.format("批量创建完成: %d 成功, %d 失败", successes.size(), failures.size()));
        response.put("successes", successes);
        response.put("failures", failures);
        return ResponseEntity.ok(response);
    }

    // ... (sync

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


    @GetMapping("/list/{appToken}")
    public ResponseEntity<?> listTables(
            @PathVariable String appToken,
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @RequestParam(required = false) String userAccessToken) {

        try {
            log.info("AppToken: {}", appToken);

            ListAppTableRespBody respBody = tableService.listTables(appToken, userAccessToken, pageSize);
            log.info("respBody: {}", respBody);
            return ResponseEntity.ok(respBody);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    @PatchMapping("/{appToken}/{tableId}")
    public ResponseEntity<?> updateTable(
            @PathVariable String appToken,
            @PathVariable String tableId,
            @RequestParam String newName,
            @RequestParam(required = false) String userAccessToken) {

        try {
            tableService.updateTable(appToken, tableId, newName, userAccessToken);

            // (可选) 更新 bitable_tables 日志表中的名称
            jdbcTemplate.update("UPDATE bitable_tables SET table_name = ? WHERE table_id = ?", newName, tableId);

            Map<String, String> response = new HashMap<>();
            response.put("message", "数据表更新成功");
            response.put("tableId", tableId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * [新功能] 删除一张数据表
     */
    @DeleteMapping("/{appToken}/{tableId}")
    public ResponseEntity<?> deleteTable(
            @PathVariable String appToken,
            @PathVariable String tableId,
            @RequestParam(required = false) String userAccessToken) {

        try {
            tableService.deleteTable(appToken, tableId, userAccessToken);

            // (推荐) 从 bitable_tables 日志表中删除记录
            jdbcTemplate.update("DELETE FROM bitable_tables WHERE table_id = ?", tableId);

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * [新功能] 批量删除数据表
     */
    @PostMapping("/batch-delete/{appToken}")
    public ResponseEntity<?> batchDeleteTables(
            @PathVariable String appToken,
            @RequestBody List<String> tableIds, // 从请求体中获取 table ID 列表
            @RequestParam(required = false) String userAccessToken) {

        try {
            BatchDeleteAppTableResp respBody = tableService.batchDeleteTables(appToken, tableIds, userAccessToken);

            // 从 bitable_tables 日志表中批量删除记录
            String inSql = String.join(",", tableIds.stream().map(id -> "?").toArray(String[]::new));
            jdbcTemplate.update(String.format("DELETE FROM bitable_tables WHERE table_id IN (%s)", inSql), tableIds.toArray());

            return ResponseEntity.ok(respBody);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * 统一的异常处理辅助方法
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception e) {
        e.printStackTrace();
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());

        if (e instanceof BitableApiException) {
            errorResponse.put("feishuCode", ((BitableApiException) e).getCode());
            errorResponse.put("feishuLogId", ((BitableApiException) e).getLogId());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}