package com.taikang.feishu.bitable.controller;

import com.lark.oapi.service.bitable.v1.model.App;
import com.lark.oapi.service.bitable.v1.model.DisplayApp;
import com.taikang.feishu.bitable.exception.BitableApiException;
import com.taikang.feishu.bitable.service.app.BitableAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/bitable/app")
public class BitableAppController {

    @Autowired
    private BitableAppService bitableAppService;

    /**
     * 【创建多维表格文档 (App)】
     *
     * @param documentName    要在飞书创建的新【文档】名称 (例如 "员工信息备份")
     * @param folderToken     (可选) 要在哪个飞书文件夹下创建
     * @param userAccessToken (可选) 如果提供, 则以此用户身份创建
     * @return 包含新 appToken 和 url 的 JSON 响应
     */
    @PostMapping("/create")
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
     * 复制一个现有的多维表格
     */
    @PostMapping("/{appToken}/copy")
    public ResponseEntity<?> copyApp(
            @PathVariable String appToken,
            @RequestParam String newName, // 副本的新名称
            @RequestParam(required = false, defaultValue = "") String folderToken,
            @RequestParam(required = false, defaultValue = "false") boolean withoutContent,
            @RequestParam(required = false) String userAccessToken) {

        try {
            App copiedApp = bitableAppService.copyApp(appToken, newName, folderToken, withoutContent, userAccessToken);
            return ResponseEntity.status(HttpStatus.CREATED).body(copiedApp); // 201 Created
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * 获取多维表格元数据
     */
    @GetMapping("/{appToken}")
    public ResponseEntity<?> getAppMetadata(
            @PathVariable String appToken,
            @RequestParam(required = false) String userAccessToken) {

        try {
            DisplayApp app = bitableAppService.getAppMetadata(appToken, userAccessToken);
            return ResponseEntity.ok(app);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    /**
     * 更新多维表格元数据 (例如: 重命名或置顶)
     */
    @PatchMapping("/{appToken}")
    public ResponseEntity<?> updateAppMetadata(
            @PathVariable String appToken,
            @RequestParam(required = false) String newName,
            @RequestParam(required = false) String userAccessToken) {

        try {
            bitableAppService.updateAppMetadata(appToken, newName, userAccessToken);
            Map<String, String> response = new HashMap<>();
            response.put("message", "更新成功");
            response.put("appToken", appToken);
            return ResponseEntity.ok(response);
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