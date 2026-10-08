package com.rahul.aifitness.ai.exception;

import com.rahul.aifitness.common.exception.BusinessException;
import com.google.genai.errors.ApiException;
import org.springframework.http.HttpStatus;

public final class AiExceptionUtil {

    private AiExceptionUtil() {
    }

    public static boolean containsApiException(Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {

            if (current instanceof ApiException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    public static BusinessException toBusinessException(
            RuntimeException exception
    ) {

        ApiException apiException = findApiException(exception);

        if (apiException == null) {
            return new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "The AI service could not process the request",
                    "AI Service Error"
            );
        }

        return switch (apiException.code()) {

            case 429 -> new BusinessException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "The AI service quota or rate limit has been exceeded. Please try again later.",
                    "AI Quota Exceeded"
            );

            case 503 -> new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service is temporarily unavailable. Please try again later.",
                    "AI Service Unavailable"
            );

            case 504 -> new BusinessException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "The AI service took too long to respond. Please try again later.",
                    "AI Service Timeout"
            );

            default -> new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "The AI service returned an unexpected error",
                    "AI Service Error"
            );
        };
    }

    private static ApiException findApiException(Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {

            if (current instanceof ApiException apiException) {
                return apiException;
            }

            current = current.getCause();
        }

        return null;
    }
}