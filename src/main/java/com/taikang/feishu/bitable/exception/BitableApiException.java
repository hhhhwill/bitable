package com.taikang.feishu.bitable.exception;

/**
 * 自定义 API 异常类
 * 继承 RuntimeException, 这样我们就不必在每个方法上都 "throws Exception"
 */
public class BitableApiException extends RuntimeException {

    private int code;
    private String logId;

    /**
     * 这就是 BitableTableService 正在调用的构造函数
     *
     * @param code  飞书 API 返回的错误码
     * @param msg   飞书 API 返回的错误信息
     * @param logId 飞书 API 返回的请求 ID (用于排查问题)
     */
    public BitableApiException(int code, String msg, String logId) {
        // 调用父类(RuntimeException)的构造函数, 将 msg 传进去
        super(msg); 
        this.code = code;
        this.logId = logId;
    }

    // Getters
    public int getCode() {
        return code;
    }

    public String getLogId() {
        return logId;
    }

    @Override
    public String getMessage() {
        // 重写 getMessage 方法, 提供更详细的错误信息
        return String.format("Bitable API Error: [code=%d, msg=%s, logId=%s]",
                this.code, super.getMessage(), this.logId);
    }
}