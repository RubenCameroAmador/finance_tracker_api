package com.pocketminder.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler (
            EmailAlreadyExistsException.class
    )
    public ResponseEntity<ErrorResponseDTO>
    handleEmailAlreadyExists(
        EmailAlreadyExistsException ex
    ){
        ErrorResponseDTO error =
                ErrorResponseDTO.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.CONFLICT.value())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO>
    handleGenericException(
            Exception ex
    ) {

        ErrorResponseDTO error =
                ErrorResponseDTO.builder()
                        .message(ex.getMessage())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(error);
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ErrorResponseDTO>
    handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        String message = Objects.requireNonNull(ex.getBindingResult()
                        .getFieldError())
                        .getDefaultMessage();

        ErrorResponseDTO error =
                ErrorResponseDTO.builder()
                        .message(message)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    @ExceptionHandler(
            UnsupportedBankException.class
    )
    public ResponseEntity<ErrorResponseDTO>
    handleUnsupportedBank(
            UnsupportedBankException ex
    ) {

        ErrorResponseDTO error =
                ErrorResponseDTO.builder()
                        .message(ex.getMessage())
                        .status(400)
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .badRequest()
                .body(error);
    }
}
