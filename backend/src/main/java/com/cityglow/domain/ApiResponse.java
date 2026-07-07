package com.cityglow.domain;

/**
 * 统一 API 响应包装(设计文档第 5.1 节)。
 *
 * <p>所有 REST 接口返回此结构,保证前端拿到统一的 {@code code/message/data} 三字段。</p>
 *
 * <p>JDK 21 Record 实现,不可变、线程安全。</p>
 *
 * @param code    业务状态码(200=成功,4xx/5xx=失败)
 * @param message 人类可读消息
 * @param data    业务数据负载,泛型
 * @param <T>     data 的类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    /**
     * 成功响应快捷工厂方法,code=200,message="success"。
     *
     * @param data 业务数据
     * @param <T>  data 类型
     * @return 成功 ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 错误响应快捷工厂方法,data=null。
     *
     * @param code    错误码(如 400/500)
     * @param message 错误消息
     * @param <T>     data 类型
     * @return 错误 ApiResponse
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
