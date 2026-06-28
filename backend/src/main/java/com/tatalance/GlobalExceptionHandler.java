package com.tatalance;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        boolean spanish = ApiMessageResolver.wantsSpanish(request);
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", ApiMessageResolver.fieldMessage(
                                fe.getField(), fe.getDefaultMessage(), spanish)
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        boolean spanish = ApiMessageResolver.wantsSpanish(request);
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        message = ApiMessageResolver.translate(message, spanish);
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("errors", List.of(Map.of("message", message))));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        boolean spanish = ApiMessageResolver.wantsSpanish(request);
        String msg = "Invalid request body";
        String detail = ex.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("Cannot deserialize value of type")) {
            if (detail.contains("PayoutType")) {
                msg = "Payout type must be one of: PERCENTAGE, FLAT";
            } else if (detail.contains("Availability")) {
                msg = "Availability must be one of: AVAILABLE, ON_TRIP, OFF_DUTY";
            } else if (detail.contains("PaymentMethod")) {
                msg = "Payment method must be one of: CASH, CARD, TRANSFER, ZELLE, VENMO, CHECK";
            }
        }
        msg = ApiMessageResolver.translate(msg, spanish);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", List.of(Map.of("field", "body", "message", msg))));
    }
}