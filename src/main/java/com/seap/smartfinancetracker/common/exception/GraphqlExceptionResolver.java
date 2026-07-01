package com.seap.smartfinancetracker.common.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Translates {@link BusinessException}s thrown inside GraphQL data fetchers into GraphQL errors.
 * <p>
 * The MVC {@link GlobalExceptionHandler} ({@code @RestControllerAdvice}) does <b>not</b> apply to
 * GraphQL: data-fetcher exceptions travel through graphql-java's own resolver chain and are
 * reported in the {@code errors} array of an HTTP 200 response, not as an HTTP status. This
 * resolver is the GraphQL-side counterpart, keeping the {@code errorCode} contract consistent
 * across both transports.
 * </p>
 */
@Slf4j
@Component
public class GraphqlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    /**
     * Maps a single thrown exception to a {@link GraphQLError}. Returning {@code null} defers to
     * the default handling (which reports a generic {@code INTERNAL_ERROR}).
     *
     * @param ex  the exception raised by a data fetcher
     * @param env the fetching environment (source, field, arguments, etc.)
     * @return the GraphQL error to report, or {@code null} to defer to the default resolver
     */
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (!(ex instanceof BusinessException businessException)) {
            // Not a domain error — let the default resolver classify it (becomes INTERNAL_ERROR).
            return null;
        }

        ErrorCode errorCode = businessException.getErrorCode();
        log.warn("GraphQL Business Exception [{}]: {}", errorCode.getCode(), errorCode.getMessage());

        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(Map.of("code", errorCode.getCode()))
                .message(errorCode.getMessage())
                .build();
    }
}
