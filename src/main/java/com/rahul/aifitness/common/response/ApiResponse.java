package com.rahul.aifitness.common.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Builder
public record ApiResponse<T> (
        boolean success,
        String message,
        T data,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data){
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    public static <T> ApiResponse<T> success(T data){
        return success("Request completed successfully", data);
    }
}
