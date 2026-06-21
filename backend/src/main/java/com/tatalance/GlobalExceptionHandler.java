package com.tatalance;

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
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", friendlyMessage(fe.getField(), fe.getDefaultMessage())
                ))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("errors", List.of(Map.of("message", message))));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errors", List.of(Map.of("field", "body", "message", msg))));
    }

    private String friendlyMessage(String field, String defaultMsg) {
        if (defaultMsg != null && defaultMsg.contains("E.164")) {
            return "Phone must start with + followed by 10-15 digits (e.g. +13055551234)";
        }
        return switch (field) {
            case "firstName" -> "First name is required";
            case "lastName" -> "Last name is required";
            case "phone" -> "Phone number is required";
            case "clientId" -> "Client is required";
            case "pickupLocation" -> "Pickup location is required";
            case "dropoffLocation" -> "Dropoff location is required";
            case "pickupDateTime" -> "Pickup date/time is required";
            case "payoutType" -> "Payout type is required (PERCENTAGE or FLAT)";
            case "payoutRate" -> "Payout rate is required";
            case "actualStart" -> "Actual start time is required";
            case "actualEnd" -> "Actual end time is required";
            case "amount" -> "Payment amount is required and must be greater than 0";
            case "method" -> "Payment method is required (CASH, CARD, TRANSFER, ZELLE, VENMO, CHECK)";
            case "date" -> "Payment date is required";
            default -> defaultMsg != null ? defaultMsg : field + " is required";
        };
    }
}
