package com.sngular.api.generator.plugin.asyncapi.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.sngular.api.generator.plugin.asyncapi.util.FactoryTypeEnum;
import com.sngular.api.generator.plugin.common.files.FileLocation;
import com.sngular.api.generator.plugin.common.tools.ApiTool;
import java.io.IOException;
import java.util.Objects;

/**
 * Utility to resolve entries inside a discriminator.mapping, supporting
 * internal references ("#/…") and external files ("./Some.yml#…").
 */
public final class DiscriminatorMappingResolver {

  private DiscriminatorMappingResolver() {
  }

  /**
   * Resolve a mapping target into a JsonNode.
   *
   * @param refValue     value from discriminator.mapping (may be internal or external)
   * @param ymlParent    base location for relative file access
   * @param rootDocument root AsyncAPI document (for internal refs)
   * @return resolved JsonNode or MissingNode if it cannot be resolved
   */
  public static JsonNode resolve(final String refValue, final FileLocation ymlParent, final JsonNode rootDocument) {
    if (Objects.isNull(refValue)) {
      return MissingNode.getInstance();
    }

    try {
      if (refValue.startsWith("#")) {
        return rootDocument.at(refValue.substring(1));
      }

      final int hashIdx = refValue.indexOf('#');
      final String fileRef = hashIdx >= 0 ? refValue.substring(0, hashIdx) : refValue;
      final String pointer = hashIdx >= 0 ? refValue.substring(hashIdx + 1) : null;

      JsonNode external = ApiTool.nodeFromFile(ymlParent, fileRef, FactoryTypeEnum.YML);
      if (pointer != null && !pointer.isEmpty()) {
        external = external.at(pointer.startsWith("/") ? pointer : "/" + pointer);
      }
      return external;
    } catch (IOException e) {
      return MissingNode.getInstance();
    }
  }
}
