package com.rural.travel.exception;

import com.rural.travel.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/** 全局异常处理:统一返回 Result JSON */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> biz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> bind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(422, msg.isEmpty() ? "参数校验失败" : msg);
    }

    /**
     * @RequestBody + @Valid 校验失败。
     * 注意: Spring 对 @RequestBody 抛的是 MethodArgumentNotValidException,
     * 不是 BindException, 必须单独处理, 否则会漏到 unknown() 变成 500。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> bodyNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(422, msg.isEmpty() ? "参数校验失败" : msg);
    }

    /**
     * 请求体 JSON 无法解析 / 类型不匹配。
     * 典型场景: 接口要对象却传了数组(如 PUT /users/interests 传 [1,2]),
     * 以前会漏到 unknown() 返回 500「系统繁忙」, 实际是客户端参数格式错误。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> notReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(422, "请求体格式不正确，请检查参数类型");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> missingParam(MissingServletRequestParameterException e) {
        return Result.fail(422, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> denied(AccessDeniedException e) {
        return Result.fail(403, "没有权限执行此操作");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> uploadTooLarge(MaxUploadSizeExceededException e) {
        return Result.fail(413, "上传文件大小超出限制(最大10MB)");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> unknown(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
