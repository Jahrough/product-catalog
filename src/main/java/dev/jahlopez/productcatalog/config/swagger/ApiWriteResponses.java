package dev.jahlopez.productcatalog.config.swagger;

import dev.jahlopez.productcatalog.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Unexpected internal server error",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiErrorResponse.class))
        )
})
public @interface ApiWriteResponses {
}