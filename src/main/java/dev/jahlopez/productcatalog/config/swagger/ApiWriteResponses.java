package dev.jahlopez.productcatalog.config.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Standardized OpenAPI response documentation for state-changing write operations (POST, PUT, PATCH, DELETE).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request payload or validation constraint violation",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "409",
                description = "Conflict due to duplicate SKU or unique constraint violation",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error during persistence or cache synchronization",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))
        )
})
public @interface ApiWriteResponses {
}