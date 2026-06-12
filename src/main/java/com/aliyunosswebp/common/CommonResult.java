package com.aliyunosswebp.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通用响应结果")
public class CommonResult<T> {

    @Schema(description = "状态码，0 表示成功", example = "0")
    private Integer code;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "提示信息", example = "success")
    private String msg;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(0);
        result.setData(data);
        result.setMsg("success");
        return result;
    }

    public static <T> CommonResult<T> error(Integer code, String msg) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
