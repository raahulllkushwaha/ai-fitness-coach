package com.rahul.aifitness.common.exception;

import org.springframework.http.HttpStatusCode;

public class BusinessException extends RuntimeException {

    private final HttpStatusCode status;
    private final String title;

    public BusinessException(
            HttpStatusCode status,
            String title,
            String message
    ) {
        super(message);
        this.status = status;
        this.title = title;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}