package com.payhub.common.result;

public class ResultUtils {

    private ResultUtils() {
    }

    public static <T> Result<T> success() {
        return new Result<>(ResultEnum.SUCCESS.getCode(), ResultEnum.SUCCESS.getMsg(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultEnum.SUCCESS.getCode(), ResultEnum.SUCCESS.getMsg(), data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultEnum.SUCCESS.getCode(), msg, data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String msg) {
        return new Result<>(resultCode.getCode(), msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> fail(ResultEnum resultEnum) {
        return new Result<>(resultEnum.getCode(), resultEnum.getMsg(), null);
    }

    public static <T> Result<T> fail(ResultEnum resultEnum, String msg) {
        return new Result<>(resultEnum.getCode(), msg, null);
    }

    public static <T> Result<T> error() {
        return fail(ResultEnum.SYSTEM_ERROR);
    }

    public static <T> Result<T> error(String msg) {
        return fail(ResultEnum.SYSTEM_ERROR, msg);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return fail(code, msg);
    }
}
