package dev.jahlopez.productcatalog.mapper;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.InjectionStrategy;

/**
 * MapStruct mapper interface providing high-performance, compile-time type-safe object mapping
 * between Product domain entities and their corresponding data transfer objects (DTOs).
 * Configured with the Spring component model and constructor injection for production readiness.
 */
@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    /**
     * Maps a persistent {@link Product} entity to a {@link ProductResponseDto}.
     *
     * @param product the source product entity instance from the database
     * @return the populated product response DTO
     */
    ProductResponseDto toResponseDto(Product product);

    /**
     * Maps an incoming {@link ProductRequestDto} payload to a new {@link Product} entity.
     * The primary key {@code id} and audit timestamps (`createdAt`, `updatedAt`) are
     * explicitly ignored as they are managed automatically by the database and JPA auditing.
     *
     * @param requestDto the incoming product request DTO containing payload details
     * @return a new unpersisted product entity instance
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(ProductRequestDto requestDto);

    /**
     * Copies mutable request fields onto an existing entity, leaving identity, version and audit fields untouched.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(ProductRequestDto requestDto, @MappingTarget Product product);
}
