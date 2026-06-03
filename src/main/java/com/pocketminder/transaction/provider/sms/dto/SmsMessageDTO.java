package com.pocketminder.transaction.provider.sms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsMessageDTO {

    @NotBlank
    private String message;

}
