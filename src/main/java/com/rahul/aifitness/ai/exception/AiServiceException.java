package com.rahul.aifitness.ai.exception;

import org.springframework.http.HttpStatusCode;

public class AiServiceException extends RuntimeException {

    private final HttpStatusCode status;

    public AiServiceException(
            HttpStatusCode status,
            String message
    ) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}