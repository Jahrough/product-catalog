package dev.jahlopez.productcatalog.exception;

import dev.jahlopez.productcatalog.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles product not found and JPA entity not found exceptions (404 Not Found).
     */
    @ExceptionHandler({ProductNotFoundException.class, jakarta.persistence.EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleProductNotFoundException(
            RuntimeException ex, HttpServletRequest request) {

        log.warn("Product not found exception occurred: {}", ex.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.NOT_FOUND.value())
                .errorCode("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles bean validation constraint violations (400 Bad Request) with structured validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiErrorResponse.ValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ApiErrorResponse.ValidationError.builder()
                        .field(fieldError.getField())
                        .rejectedMessage(fieldError.getDefaultMessage())
                        .build())
                .toList();

        log.warn("Validation failed for request path [{}]: {} field errors detected", request.getRequestURI(), validationErrors.size());

        return badRequest(request, "Validation failed for one or more fields.", validationErrors);
    }

    /**
     * Handles malformed JSON and request values that cannot be converted to the expected type.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(
            Exception ex, HttpServletRequest request) {
        return badRequest(request, "The request contains a malformed body or invalid parameter.", null);
    }

    /**
     * Catches all unhandled system exceptions (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected internal server error occurred at path [{}]: ", request.getRequestURI(), ex);

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred.")
                .path(request.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> badRequest(
            HttpServletRequest request,
            String message,
            List<ApiErrorResponse.ValidationError> validationErrors) {
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("INVALID_REQUEST_PAYLOAD")
                .message(message)
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}