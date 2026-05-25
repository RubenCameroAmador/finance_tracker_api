package com.pocketminder.common.exception;

public class EmailAlreadyExistsException extends RuntimeException{

    public EmailAlreadyExistsException(
            String message
    ){
        super(message);
    }
}
