package com.taikang.feishu.bitable.controller;

import com.lark.oapi.service.bitable.v1.model.App;
import com.taikang.feishu.bitable.service.app.BitableAppService;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 【API 1: 创建多维表格文档 (App)】
     * 调用此 API 创建一个新的、空的多维表格文档。
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

    // TODO: 在这里添加您未来的 "复制App"、"获取App元数据" 等 API
}