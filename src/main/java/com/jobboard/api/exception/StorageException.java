package com.jobboard.api.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends ApiException {

    public StorageException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
