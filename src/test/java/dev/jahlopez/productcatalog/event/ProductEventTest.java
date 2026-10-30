package dev.jahlopez.productcatalog.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductEventTest {

    @Test
    void builder_rejectsNullVersion() {
        assertThatThrownBy(() -> ProductEvent.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event version must be a positive integer");
    }

    @Test
    void builder_rejectsNonPositiveVersion() {
        assertThatThrownBy(() -> ProductEvent.builder().version(0L).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Event version must be a positive integer");
    }
}
