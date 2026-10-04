package com.rahul.aifitness.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "Resource not found. path={}, message={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ProblemDetail problem = createProblem(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request,
                "resource-not-found"
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }


    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "Duplicate resource. path={}, message={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ProblemDetail problem = createProblem(
                HttpStatus.CONFLICT,
                "Resource Conflict",
                ex.getMessage(),
                request,
                "resource-conflict"
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }


    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "Business exception. path={}, message={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        ProblemDetail problem = createProblem(
                ex.getStatus(),
                ex.getTitle(),
                ex.getMessage(),
                request,
                "business-rule-violation"
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            org.springframework.http.HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                "One or more request fields are invalid."
        );

        problem.setTitle("Validation Failed");

        problem.setType(
                URI.create(
                        "https://api.aifitness.com/problems/validation-failed"
                )
        );

        problem.setProperty("timestamp", nowUtc());
        problem.setProperty("errors", errors);
        problem.setProperty("errorId", UUID.randomUUID().toString());

        return handleExceptionInternal(
                ex,
                problem,
                headers,
                status,
                request
        );
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedRequest(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "Malformed request body. path={}",
                request.getRequestURI()
        );

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                "The request body is invalid or could not be parsed.",
                request,
                "malformed-request"
        );

        return ResponseEntity.badRequest().body(problem);
    }


    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem = createProblem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                "The HTTP method is not supported for this resource.",
                request,
                "method-not-allowed"
        );

        if (ex.getSupportedHttpMethods() != null) {
            problem.setProperty(
                    "supportedMethods",
                    ex.getSupportedHttpMethods()
                            .stream()
                            .map(HttpMethod::name)
                            .toList()
            );
        }

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(problem);
    }


    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            org.springframework.http.HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        String path = ex.getRequestURL();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                "No endpoint exists for path: " + path
        );

        problem.setTitle("Endpoint Not Found");

        problem.setType(
                URI.create(
                        "https://api.aifitness.com/problems/endpoint-not-found"
                )
        );

        problem.setProperty("timestamp", nowUtc());
        problem.setProperty("errorId", UUID.randomUUID().toString());

        return handleExceptionInternal(
                ex,
                problem,
                headers,
                status,
                request
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {

        String errorId = UUID.randomUUID().toString();

        log.error(
                "Unexpected exception. errorId={}, path={}",
                errorId,
                request.getRequestURI(),
                ex
        );

        ProblemDetail problem = createProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request,
                "internal-server-error"
        );

        problem.setProperty("errorId", errorId);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problem);
    }


    private ProblemDetail createProblem(
            HttpStatusCode status,
            String title,
            String detail,
            HttpServletRequest request,
            String problemType
    ) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        problem.setType(
                URI.create(
                        "https://api.aifitness.com/problems/" + problemType
                )
        );

        problem.setInstance(
                URI.create(request.getRequestURI())
        );

        problem.setProperty("timestamp", nowUtc());
        problem.setProperty(
                "errorId",
                UUID.randomUUID().toString()
        );

        return problem;
    }

    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}