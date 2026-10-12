package dev.jahlopez.productcatalog.mapper;

import dev.jahlopez.productcatalog.dto.ProductRequestDto;
import dev.jahlopez.productcatalog.dto.ProductResponseDto;
import dev.jahlopez.productcatalog.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Converts Entity to Response DTO
    ProductResponseDto toResponseDto(Product product);

    // Converts Request DTO to Entity (ignoring ID since it's auto-generated)
    @Mapping(target = "id", ignore = true)
    Product toEntity(ProductRequestDto requestDto);
}