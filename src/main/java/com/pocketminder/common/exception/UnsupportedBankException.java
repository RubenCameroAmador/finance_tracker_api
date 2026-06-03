package com.pocketminder.common.exception;

public class UnsupportedBankException
        extends RuntimeException {

    public UnsupportedBankException(
            String message
    ) {
        super(message);
    }
}