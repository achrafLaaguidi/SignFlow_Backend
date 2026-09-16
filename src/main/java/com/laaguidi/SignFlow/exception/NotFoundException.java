package com.laaguidi.SignFlow.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends RuntimeException  {
    public NotFoundException(String message) {
        super(message);
    }
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }


}
