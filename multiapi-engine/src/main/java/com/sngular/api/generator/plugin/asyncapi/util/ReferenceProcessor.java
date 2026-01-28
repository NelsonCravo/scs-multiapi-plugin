package com.sngular.api.generator.plugin.asyncapi.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.sngular.api.generator.plugin.asyncapi.exception.FileSystemException;
import com.sngular.api.generator.plugin.common.files.FileLocation;
import com.sngular.api.generator.plugin.common.files.DirectoryFileLocation;
import com.sngular.api.generator.plugin.common.tools.ApiTool;
import com.sngular.api.generator.plugin.common.tools.MapperUtil;
import com.sngular.api.generator.plugin.common.tools.PathUtil;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

public final class ReferenceProcessor {

  private static final String JSON = "json";

  private static final String YML = "yml";

  private static final String YAML = "yaml";

  private static final String REF = "$ref";

  private static final String AVSC = "avsc";

  private final FileLocation ymlParent;

  private final Map<String, JsonNode> totalSchemas;

  private List<String> alreadyProcessed;

  @Builder
  public ReferenceProcessor(final FileLocation ymlParent, final Map<String, JsonNode> totalSchemas) {
    this.ymlParent = ymlParent;
    this.totalSchemas = totalSchemas;
  }

  public void processReference(final JsonNode node, final String referenceLink) {
    processReference(node, referenceLink, ymlParent);
  }

  private void processReference(final JsonNode node, final String referenceLink, final FileLocation currentParent) {
    if (alreadyProcessed == null) {
      alreadyProcessed = new ArrayList<>();
    }
    final String[] path = MapperUtil.splitReference(referenceLink);
    final JsonNode component;
    final var calculatedKey = MapperUtil.getRefSchemaKey(referenceLink);
    if (!totalSchemas.containsKey(calculatedKey) && !alreadyProcessed.contains(calculatedKey)) {
      alreadyProcessed.add(calculatedKey);
      try {
        if (referenceLink.toLowerCase().contains(YML) || referenceLink.toLowerCase().contains(YAML) || referenceLink.toLowerCase().contains(JSON)) {
          final FileLocation targetParent = resolveParent(currentParent, path[0]);
          component = solveRef(targetParent, path[0], path[1], totalSchemas);
          // Ensure nested references are resolved relative to the external file we just loaded
          currentParent = targetParent;
        } else {
          if (referenceLink.toLowerCase().contains(AVSC)) {
            component = solveRef(currentParent, path[0], path[1], totalSchemas);
          } else {
            component = node.at(MapperUtil.getPathToModel(referenceLink)).get(MapperUtil.getModel(referenceLink));
          }
        }
      } catch (final IOException e) {
        throw new FileSystemException(e);
      }
      if (Objects.nonNull(component)) {
        totalSchemas.put(calculatedKey, component);
        // Resolve nested references using the correct parent (the file that declared them)
        checkReference(node, component, currentParent);
      }
    }
  }

  private JsonNode solveRef(final FileLocation ymlParent, final String path, final String reference, final Map<String, JsonNode> totalSchemas) throws IOException {
    JsonNode returnNode = null;

    if (path.endsWith(YML) || path.endsWith(JSON) || path.endsWith(YAML)) {
      final JsonNode node = ApiTool.nodeFromFile(ymlParent, path, FactoryTypeEnum.YML);
      if (StringUtils.isNotEmpty(reference)) {
        if (node.at(MapperUtil.getPathToModel(reference)).has(MapperUtil.getModel(reference))) {
          returnNode = node.at(MapperUtil.getPathToModel(reference)).get(MapperUtil.getModel(reference));
          checkReference(node, returnNode, ymlParent);
        } else {
          returnNode = node;
        }
      } else {
        returnNode = node;
      }
    } else if (path.endsWith(AVSC)) {
      returnNode = ApiTool.nodeFromFile(ymlParent, path, FactoryTypeEnum.AVRO);
    } else {
      returnNode = totalSchemas.getOrDefault(MapperUtil.getRefSchemaKey(reference), null);
    }
    return returnNode;
  }

  private void checkReference(
      final JsonNode mainNode, final JsonNode node, final FileLocation currentParent) {
    final var localReferences = node.findValues(REF);
    if (!localReferences.isEmpty()) {
      localReferences.forEach(localReference -> processReference(mainNode, ApiTool.getNodeAsString(localReference), currentParent));
    }
  }

  private FileLocation resolveParent(final FileLocation baseParent, final String path) {
    Path resolvedPath;
    if (PathUtil.isAbsolutePath(path)) {
      resolvedPath = Paths.get(path).normalize();
    } else {
      resolvedPath = Paths.get(baseParent.path()).resolve(path).normalize();
    }
    return new DirectoryFileLocation(resolvedPath.getParent());
  }
}
