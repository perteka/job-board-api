package com.jobboard.api.exception;

import com.jobboard.api.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return build(exception.getStatus(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Doğrulama hatası", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Doğrulama hatası", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Geçersiz istek gövdesi", request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "İşlem veritabanı kısıtıyla çakışıyor", request, Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Dosya boyutu izin verilen sınırı aşıyor", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Geçersiz parametre: " + exception.getName(), request, Map.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Eksik parametre: " + exception.getParameterName(), request, Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Kaynak bulunamadı", request, Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP metodu desteklenmiyor", request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok", request, Map.of());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(
            PropertyReferenceException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Geçersiz alan referansı", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Beklenmeyen hata", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir sunucu hatası oluştu", request, Map.of());
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.status(status).body(response);
    }
}
