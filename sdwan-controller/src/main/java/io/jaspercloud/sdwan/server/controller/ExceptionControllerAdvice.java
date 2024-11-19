package io.jaspercloud.sdwan.server.controller;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<String> onNotLoginException(NotLoginException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
    }

    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<String> onNotRoleException(NotRoleException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.badRequest().body("无权访问的接口");
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<String> onNotPermissionException(NotPermissionException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.badRequest().body("无权访问的接口");
    }

    @ExceptionHandler(ProcessException.class)
    public ResponseEntity<String> onProcessException(ProcessException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(ProcessCodeException.class)
    public ResponseEntity<String> onProcessCodeException(ProcessCodeException e) {
        log.error(e.getMessage(), e);
        String message = SDWanProtos.MessageCode.forNumber(e.getCode()).name();
        switch (e.getCode()) {
            case SDWanProtos.MessageCode.NotGrant_VALUE:
                message = "未授权";
                break;
            case SDWanProtos.MessageCode.Disabled_VALUE:
                message = "被禁用";
                break;
            case SDWanProtos.MessageCode.NotEnough_VALUE:
                message = "vip资源不足";
                break;
        }
        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        FieldError fieldError = e.getBindingResult().getFieldError();
        String field = fieldError.getField();
        String message = fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(String.format("%s: %s", field, message));
    }
}
