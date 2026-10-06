package com.deepblue.rescue.exception;

import com.deepblue.rescue.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce excepciones de aplicación a un contrato HTTP consistente:
 * todas las respuestas de error son ResponseEntity&lt;ErrorResponse&gt;.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // 409
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // 400 - Bean Validation (@Valid sobre @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Ordenado para que, si un campo falla varias reglas, el mensaje elegido sea determinista
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField)
                        .thenComparing(error -> messageOf(error)))
                .toList();

        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : fieldErrors) {
            details.putIfAbsent(error.getField(), messageOf(error));
        }
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    // 400 - JSON mal formado o enum inexistente en el body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or invalid JSON request",
                Map.of("body", "Check JSON syntax and enum values"));
    }

    // 400 - query param / path variable con valor inválido (p. ej. ?status=FLYING)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid request parameter",
                Map.of(ex.getName(), "Invalid value: " + ex.getValue()));
    }

    // 500 - cualquier otro error. No se expone stack trace ni detalles internos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        // Las excepciones del propio framework (parámetro obligatorio ausente, 405, 415,
        // ruta inexistente...) ya traen su código HTTP: se respeta en vez de convertirlo en 500.
        if (ex instanceof org.springframework.web.ErrorResponse frameworkError) {
            HttpStatus status = HttpStatus.resolve(frameworkError.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                String detail = frameworkError.getBody().getDetail();
                return build(status, detail != null ? detail : status.getReasonPhrase(), Map.of());
            }
        }

        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of());
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
    }

    private static ResponseEntity<ErrorResponse> build(
            HttpStatus status, String message, Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details);
        return ResponseEntity.status(status).body(body);
    }
}
