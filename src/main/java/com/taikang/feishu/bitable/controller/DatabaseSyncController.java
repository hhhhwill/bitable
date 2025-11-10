package com.taikang.feishu.bitable.controller;

// --- 修正点 1: 导入正确的 App 类型 ---
import com.lark.oapi.service.bitable.v1.model.App;
import com.taikang.feishu.bitable.service.BitableAppService;
import com.taikang.feishu.bitable.service.DatabaseSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于管理飞书多维表格 App 和 Table 的 Controller
 * (重构了逻辑, 分离了 App 和 Table 的创建)
 */
@RestController
@RequestMapping("/api/bitable") // 修改了基础路径, 更清晰
public class DatabaseSyncController {

    @Autowired
    private DatabaseSyncService syncService;

    @Autowired
    private BitableAppService bitableAppService;

    /**
     * 【API 1: 创建多维表格文档 (App)】
     * 调用此 API 创建一个新的、空的多维表格文档。
     *
     * @param documentName    要在飞书创建的新【文档】名称 (例如 "员工信息备份")
     * @param folderToken     (可选) 要在哪个飞书文件夹下创建
     * @param userAccessToken (可选) 如果提供, 则以此用户身份创建
     * @return 包含新 appToken 和 url 的 JSON 响应
     */
    @PostMapping("/app")
    public ResponseEntity<Map<String, Object>> createBitableApp(
            @RequestParam String documentName,
            @RequestParam(required = false, defaultValue = "") String folderToken,
            @RequestParam(required = false) String userAccessToken) {

        try {
            // 步骤 1: 调用 BitableAppService 创建一个新的多维表格【文档】
            App newApp = bitableAppService.createBitableApp(documentName, folderToken, userAccessToken);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "多维表格文档创建成功");
            response.put("app", newApp); // 返回官方的完整 App 对象
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 【API 2: 同步数据库表到指定文档 (Table)】
     * 调用此 API, 将本地数据库表同步到【已存在】的多维表格文档中, 创建一张新【数据表】。
     *
     * @param appToken          【必需】飞书 Bitable App Token (bascn...)
     * @param dbTableName       要同步的数据库表名 (例如 "employees")
     * @param newBitableTableName 在飞书中创建的新【数据表】名
     * @return 包含 tableId 的 JSON 响应
     */
    @PostMapping("/table")
    public ResponseEntity<Map<String, Object>> syncTableToBitable(
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
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}