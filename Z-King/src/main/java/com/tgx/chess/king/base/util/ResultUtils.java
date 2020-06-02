package com.tgx.chess.king.base.util;

public class ResultUtils
{

    private static final int    SUCCESS_CODE         = 200;
    private static final String SUCCESS_MESSAGE      = "成功";
    private static final int    UNAUTHORIZED_CODE    = 401;
    private static final String UNAUTHORIZED_MESSAGE = "无访问权限";

    public static <T> Result<T> success(T data)
    {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage(SUCCESS_MESSAGE);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success()
    {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message)
    {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> unauthorized()
    {
        Result<T> result = new Result<>();
        result.setCode(UNAUTHORIZED_CODE);
        result.setMessage(UNAUTHORIZED_MESSAGE);
        return result;
    }
}
