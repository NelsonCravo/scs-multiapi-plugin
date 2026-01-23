package com.sngular.api.generator.plugin.asyncapi.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameUtilsTest {

  @Test
  void withSuffixShouldAppendWhenMissing() {
    assertThat(NameUtils.withSuffix("Order", "Supplier")).isEqualTo("OrderSupplier");
  }

  @Test
  void withSuffixShouldAvoidDuplicatingExistingSuffix() {
    assertThat(NameUtils.withSuffix("OrderSupplier", "Supplier")).isEqualTo("OrderSupplier");
  }

  @Test
  void withPrefixAndSuffixShouldNotDuplicateSuffix() {
    assertThat(NameUtils.withPrefixAndSuffix("Async", "OrderSupplier", "Supplier"))
        .isEqualTo("AsyncOrderSupplier");
  }
}
