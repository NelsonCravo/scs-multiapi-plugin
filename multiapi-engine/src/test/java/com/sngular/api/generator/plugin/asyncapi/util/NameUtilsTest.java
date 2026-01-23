package com.sngular.api.generator.plugin.asyncapi.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameUtilsTest {

  @Test
  void withSuffixShouldAppendEvenWhenAlreadyPresent() {
    assertThat(NameUtils.withSuffix("OrderSupplier", "Supplier")).isEqualTo("OrderSupplierSupplier");
  }

  @Test
  void withSuffixShouldAppendWhenMissing() {
    assertThat(NameUtils.withSuffix("Order", "Supplier")).isEqualTo("OrderSupplier");
  }

  @Test
  void withOneSuffixShouldAvoidDuplicatingExistingSuffix() {
    assertThat(NameUtils.withOneSuffix("OrderSupplier", "Supplier")).isEqualTo("OrderSupplier");
  }

  @Test
  void withPrefixAndSuffixShouldNotDuplicateSuffix() {
    assertThat(NameUtils.withPrefixAndSuffix("Async", "OrderSupplier", "Supplier"))
        .isEqualTo("AsyncOrderSupplier");
  }
}
