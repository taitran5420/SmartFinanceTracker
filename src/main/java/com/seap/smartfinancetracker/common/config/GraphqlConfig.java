package com.seap.smartfinancetracker.common.config;

import graphql.scalars.ExtendedScalars;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Registers the custom GraphQL scalars used by the analytics schema.
 * <p>
 * graphql-java only ships {@code Int}, {@code Float}, {@code String}, {@code Boolean} and
 * {@code ID} out of the box, so the domain scalars must be wired explicitly:
 * <ul>
 *     <li>{@code DateTime} — ISO-8601, bound to {@link java.time.OffsetDateTime}</li>
 *     <li>{@code BigDecimal} — exact money amounts (never {@code Float})</li>
 *     <li>{@code Long} — 64-bit counts; {@code transactionCount} is a Java {@code long} and the
 *         built-in {@code Int} is only 32-bit</li>
 *     <li>{@code UUID} — entity identifiers</li>
 * </ul>
 * ({@code percentage} maps to the built-in {@code Float}; {@code year}/{@code month} to {@code Int}.)
 * </p>
 */
@Configuration
public class GraphqlConfig {

    /**
     * Adds the extended scalars to the runtime wiring so the schema's scalar declarations resolve.
     *
     * @return a configurer contributing the custom scalars
     */
    @Bean
    public RuntimeWiringConfigurer scalarConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.UUID);
    }
}
