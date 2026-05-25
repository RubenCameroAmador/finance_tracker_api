package com.pocketminder.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponseDTO {

    private String message;

    private int status;

    private LocalDateTime timestamp;
}
