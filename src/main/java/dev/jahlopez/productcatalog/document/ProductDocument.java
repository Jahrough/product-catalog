package dev.jahlopez.productcatalog.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * Elasticsearch document representation of catalog items optimized for full-text search and faceting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
public class ProductDocument {

    /**
     * Unique identifier for the Elasticsearch document (mirrors relational product ID).
     */
    @Id
    private Long id;

    /**
     * Stock Keeping Unit (SKU) used for precise product lookup and exact-match filtering.
     */
    @Field(type = FieldType.Keyword)
    private String sku;

    /**
     * Product name optimized for full-text search with standard text analysis.
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    /**
     * Product description optimized for full-text search with standard text analysis.
     */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    /**
     * Product pricing represented as a double precision numeric value.
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * Available inventory stock quantity.
     */
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;
}