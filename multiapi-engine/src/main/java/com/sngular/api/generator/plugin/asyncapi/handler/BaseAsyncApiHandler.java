package com.sngular.api.generator.plugin.asyncapi.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sngular.api.generator.plugin.asyncapi.exception.DuplicateClassException;
import com.sngular.api.generator.plugin.asyncapi.model.ProcessBindingsResult;
import com.sngular.api.generator.plugin.asyncapi.model.ProcessMethodResult;
import com.sngular.api.generator.plugin.asyncapi.parameter.OperationParameterObject;
import com.sngular.api.generator.plugin.asyncapi.parameter.SpecFile;
import com.sngular.api.generator.plugin.asyncapi.template.TemplateFactory;
import com.sngular.api.generator.plugin.common.files.ClasspathFileLocation;
import com.sngular.api.generator.plugin.common.files.DirectoryFileLocation;
import com.sngular.api.generator.plugin.common.files.FileLocation;
import com.sngular.api.generator.plugin.common.model.CommonSpecFile;
import com.sngular.api.generator.plugin.common.model.SchemaObject;
import com.sngular.api.generator.plugin.common.tools.ApiTool;
import com.sngular.api.generator.plugin.exception.InvalidAPIException;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public abstract class BaseAsyncApiHandler {

  protected static final String PACKAGE_SEPARATOR_STR = ".";

  protected static final String SLASH = "/";

  protected static final Pattern PACKAGE_SEPARATOR = Pattern.compile(PACKAGE_SEPARATOR_STR);

  protected static final String DEFAULT_ASYNCAPI_API_PACKAGE = "com.sngular.api.asyncapi";

  protected static final String DEFAULT_ASYNCAPI_MODEL_PACKAGE = DEFAULT_ASYNCAPI_API_PACKAGE + ".model";

  protected static final String CONSUMER_CLASS_NAME = "Subscriber";

  protected static final String SUPPLIER_CLASS_NAME = "Producer";

  protected static final String STREAM_BRIDGE_CLASS_NAME = "StreamBridgeProducer";

  protected static final String SUBSCRIBE = "subscribe";

  protected static final String PUBLISH = "publish";

  protected static final String ACTION = "action";

  protected static final String PARAMETERS = "parameters";

  protected static final String CORRELATION_ID = "correlationId";

  protected static final String REPLY_TO = "replyTo";

  protected static final String CAUSATION_ID = "causationId";

  protected static final String SCHEMA_FORMAT = "schemaFormat";

  protected static final String SCHEMA_VERSION = "schemaVersion";

  protected static final String PROPERTIES = "properties";

  protected static final String BINDING_VERSION = "bindingVersion";

  protected static final String REPLY_SUFFIX = "-reply";

  protected static final String CHANNEL_REGEX = "[a-zA-Z0-9._/#{}:+\\-]*";

  protected static final String ACKS = "acks";

  protected static final String PARTITIONS = "partitions";

  protected static final String HEADERS = "headers";

  protected static final String TOPIC_CONFIGURATION = "topicConfiguration";

  protected static final String EXAMPLES = "examples";

  protected static final String X_KEY_SELECTOR = "x-keySelector";

  protected static final String MQTT = "mqtt";

  protected static final String WS = "ws";

  protected static final String WEBSOCKETS = "websockets";

  protected static final String QOS = "qos";

  protected static final String RETAIN = "retain";

  protected static final String METHOD = "method";

  protected static final String SUBPROTOCOL = "subprotocol";

  protected static final String OPERATION_ID = "operationId";

  protected static final String AVSC = "avsc";

  protected static final String PAYLOAD = "payload";

  protected static final String REF = "$ref";

  protected static final String MESSAGES = "messages";

  protected static final String EVENT = "event";

  protected static final String MESSAGE = "message";

  protected static final String SCHEMAS = "schemas";

  protected static final String CHANNELS = "channels";

  protected static final String BINDINGS = "bindings";

  protected static final String KAFKA = "kafka";

  protected static final String KEY = "key";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  protected final List<String> processedOperationIds = new ArrayList<>();

  protected final List<String> processedClassnames = new ArrayList<>();

  protected final List<String> processedApiPackages = new ArrayList<>();

  protected final Path baseDir;

  protected final TemplateFactory templateFactory;

  protected final String groupId;

  protected final Integer springBootVersion;

  protected JsonNode currentRoot;

  protected BaseAsyncApiHandler(
      final Integer springBootVersion,
      boolean overwriteModel,
      final File targetFolder,
      final String processedGeneratedSourcesFolder,
      final String groupId,
      final File baseDir) {
    this.groupId = groupId;
    this.baseDir = baseDir.toPath().toAbsolutePath();
    this.templateFactory = new TemplateFactory(overwriteModel, targetFolder, processedGeneratedSourcesFolder, baseDir);
    this.springBootVersion = springBootVersion;
  }

  protected static FileLocation resolveYmlLocation(final String ymlFilePath) throws IOException, URISyntaxException {
    final var classPathInput = BaseAsyncApiHandler.class.getClassLoader().getResource(ymlFilePath);
    if (Objects.nonNull(classPathInput)) {
      return new ClasspathFileLocation(getParentUri(classPathInput.toURI()));
    }

    final File f = new File(ymlFilePath);
    if (f.exists()) {
      return new DirectoryFileLocation(f.toPath().getParent());
    }

    throw new FileNotFoundException("Could not find YAML file: " + ymlFilePath);
  }

  public static URI getParentUri(URI uri) {
    if ("jar".equals(uri.getScheme())) {
      // Split "jar:file:/path/to/app.jar!/dir/file.txt"
      String[] parts = uri.getSchemeSpecificPart().split("!", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid JAR URI: " + uri);
      }

      String jarPath = parts[0];
      Path innerPath = Paths.get(parts[1]);
      Path parentPath = innerPath.getParent();

      if (parentPath == null) {
        throw new IllegalArgumentException("No parent path inside JAR for: " + uri);
      }

      return URI.create("jar:" + jarPath + "!" + parentPath.toString().replace("\\", "/"));
    } else if ("file".equals(uri.getScheme())) {
      Path path = Paths.get(uri);
      Path parent = path.getParent();
      if (parent == null) {
        throw new IllegalArgumentException("No parent for file URI: " + uri);
      }
      return parent.toUri();
    } else {
      throw new IllegalArgumentException("Unsupported URI scheme: " + uri.getScheme());
    }
  }

  public abstract void processFileSpec(final List<SpecFile> specsListFile);

  protected abstract Map<String, JsonNode> getAllSchemas(final FileLocation ymlParent, final JsonNode node);

  protected abstract void processOperation(
      final SpecFile fileParameter, final FileLocation ymlParent, final Map.Entry<String, JsonNode> entry, final JsonNode channel,
      final String operationId, final JsonNode channelPayload, final Map<String, JsonNode> totalSchemas, final JsonNode root) throws IOException, TemplateException;

  protected abstract void processSupplierMethod(
      final String operationId, final JsonNode channel, final OperationParameterObject operationObject, final FileLocation ymlParent,
      final Map<String, JsonNode> totalSchemas, final JsonNode channelNode, final JsonNode root, final String channelName)
      throws IOException, TemplateException;

  protected abstract void processStreamBridgeMethod(
      final String operationId, final JsonNode channel, final OperationParameterObject operationObject, final FileLocation ymlParent, final String channelName,
      final Map<String, JsonNode> totalSchemas, final JsonNode channelNode, final JsonNode root)
      throws IOException, TemplateException;

  protected abstract void processSubscribeMethod(
      final String operationId, final JsonNode channel, final OperationParameterObject operationObject, final FileLocation ymlParent,
      final Map<String, JsonNode> totalSchemas, final JsonNode channelNode, final JsonNode root, final String channelName) throws IOException, TemplateException;

  protected abstract void fillTemplateFactory(
      final String operationId,
      final ProcessMethodResult processedMethod, final Map<String, JsonNode> totalSchemas, final OperationParameterObject operationObject)
      throws IOException;

  protected abstract ProcessMethodResult processMethod(
      final String operationId,
      final JsonNode channel, final OperationParameterObject operationObject, final FileLocation ymlParent, final Map<String, JsonNode> totalSchemas,
      final String channelBindings, final String operationBindings, final String serverBindings, final String securityRequirements, final String securitySchemes,
      final String channelParameters)
      throws IOException;

  protected String applySchemaVersionNamespace(final String namespace, final String schemaVersion) {
    if (StringUtils.isBlank(schemaVersion) || StringUtils.isBlank(namespace)) {
      return namespace;
    }
    final String sanitized = schemaVersion.replaceAll("[^A-Za-z0-9]", "_");
    return namespace + "_v" + sanitized;
  }

  protected abstract Pair<String, JsonNode> processPayload(
      final OperationParameterObject operationObject, final String messageName, final JsonNode payload, final FileLocation ymlParent)
      throws IOException;

  protected abstract Pair<String, JsonNode> processMethodRef(
      final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final String messageRef, final OperationParameterObject operationObject,
      final FileLocation ymlParent, final Map<String, JsonNode> totalSchemas, final JsonNode method) throws IOException;

  protected abstract String processMessageRef(final JsonNode messageBody, final String modelPackage, final FileLocation ymlParent) throws IOException;

  protected abstract String processExternalAvro(final FileLocation ymlParent, final String messageContent) throws IOException;

  protected abstract String processExternalRef(final String modelPackage, final FileLocation ymlParent, final JsonNode message) throws IOException;

  protected abstract void processBindings(
      final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final JsonNode message,
      final CommonSpecFile commonSpecFile);

  protected abstract void processKafkaBindings(
      final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final JsonNode kafkaBindings, final CommonSpecFile specFile);

  protected abstract String processModelPackage(final String extractedPackage, final String modelPackage);

  protected void validateKafkaBindingVersion(final String bindingVersion) {
    if (StringUtils.isBlank(bindingVersion)) {
      return;
    }
    final String[] parts = bindingVersion.split("\\.");
    try {
      final int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
      final int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
      if (major > 0 || minor > 4) {
        log.warn("Kafka bindingVersion '{}' is newer than supported (<=0.4.x). Generated code may not cover all fields.", bindingVersion);
      }
    } catch (final NumberFormatException e) {
      log.warn("Kafka bindingVersion '{}' is not parseable; proceeding with defaults.", bindingVersion);
    }
  }

  protected Integer extractPartitionId(final JsonNode partitionsNode) {
    if (Objects.isNull(partitionsNode)) {
      return null;
    }
    if (partitionsNode.isInt()) {
      return partitionsNode.asInt();
    }
    if (partitionsNode.isArray() && partitionsNode.size() > 0) {
      final JsonNode first = partitionsNode.get(0);
      if (first.isInt()) {
        return first.asInt();
      }
      if (first.has("partition") && first.get("partition").isInt()) {
        return first.get("partition").asInt();
      }
    }
    if (partitionsNode.has("partition") && partitionsNode.get("partition").isInt()) {
      return partitionsNode.get("partition").asInt();
    }
    return null;
  }

  protected void setUpTemplate(final SpecFile fileParameter, final Integer springBootVersion) {
    processPackage(fileParameter);
    templateFactory.processFilePaths(fileParameter, DEFAULT_ASYNCAPI_API_PACKAGE);
    processClassNames(fileParameter);
    processEntitiesSuffix(fileParameter);
    processJavaEEPackage(springBootVersion);
  }

  protected void processPackage(final SpecFile fileParameter) {
    if (ObjectUtils.anyNotNull(fileParameter.getSupplier(), fileParameter.getStreamBridge(), fileParameter.getConsumer())) {
      templateFactory.setSupplierPackageName(evaluatePackage(fileParameter.getSupplier()));
      templateFactory.setStreamBridgePackageName(evaluatePackage(fileParameter.getStreamBridge()));
      templateFactory.setSubscribePackageName(evaluatePackage(fileParameter.getConsumer()));
    } else {
      throw new InvalidAPIException("No Configuration provided, nothing will be generated.");
    }
  }

  protected void processClassNames(final SpecFile fileParameter) {
    templateFactory.setSupplierClassName(fileParameter.getSupplier() != null && fileParameter.getSupplier().getClassNamePostfix() != null
                                             ? fileParameter.getSupplier().getClassNamePostfix() : SUPPLIER_CLASS_NAME);
    templateFactory.setStreamBridgeClassName(fileParameter.getStreamBridge() != null && fileParameter.getStreamBridge().getClassNamePostfix() != null
                                                 ? fileParameter.getStreamBridge().getClassNamePostfix() : STREAM_BRIDGE_CLASS_NAME);
    templateFactory.setSubscribeClassName(fileParameter.getConsumer() != null && fileParameter.getConsumer().getClassNamePostfix() != null
                                              ? fileParameter.getConsumer().getClassNamePostfix() : CONSUMER_CLASS_NAME);
  }

  protected void processEntitiesSuffix(final SpecFile fileParameter) {
    templateFactory.setSupplierEntitiesSuffix(null);
    templateFactory.setStreamBridgeEntitiesSuffix(null);
    templateFactory.setSubscribeEntitiesSuffix(null);
  }

  protected void processJavaEEPackage(final Integer springBootVersion) {
    templateFactory.calculateJavaEEPackage(springBootVersion);
  }

  protected String stringify(final JsonNode node) {
    return Objects.nonNull(node) ? node.toString() : null;
  }

  protected String extractServerBindings(final JsonNode root, final JsonNode operation) {
    if (Objects.isNull(root) || !ApiTool.hasNode(root, "servers")) {
      return null;
    }
    final JsonNode serversNode = ApiTool.getNode(root, "servers");
    final ObjectNode collected = MAPPER.createObjectNode();

    if (ApiTool.hasNode(operation, "servers")) {
      final Iterator<JsonNode> serverNames = ApiTool.getNode(operation, "servers").elements();
      while (serverNames.hasNext()) {
        final String serverName = serverNames.next().asText();
        if (serversNode.has(serverName) && ApiTool.hasNode(serversNode.get(serverName), BINDINGS)) {
          collected.set(serverName, ApiTool.getNode(serversNode.get(serverName), BINDINGS));
        }
      }
    }

    if (!collected.fieldNames().hasNext()) {
      final Iterator<Map.Entry<String, JsonNode>> serverEntries = serversNode.fields();
      while (serverEntries.hasNext()) {
        final Map.Entry<String, JsonNode> serverEntry = serverEntries.next();
        if (ApiTool.hasNode(serverEntry.getValue(), BINDINGS)) {
          collected.set(serverEntry.getKey(), ApiTool.getNode(serverEntry.getValue(), BINDINGS));
        }
      }
    }

    return collected.fieldNames().hasNext() ? collected.toString() : null;
  }

  protected Pair<String, String> extractSecurity(final JsonNode root, final JsonNode operation) {
    final var requirementsArray = MAPPER.createArrayNode();
    if (ApiTool.hasNode(operation, "security")) {
      ApiTool.getNode(operation, "security").forEach(requirementsArray::add);
    } else if (ApiTool.hasNode(root, "security")) {
      ApiTool.getNode(root, "security").forEach(requirementsArray::add);
    }

    if (ApiTool.hasNode(operation, "servers") && ApiTool.hasNode(root, "servers")) {
      final JsonNode serversNode = ApiTool.getNode(root, "servers");
      final Iterator<JsonNode> serverNames = ApiTool.getNode(operation, "servers").elements();
      while (serverNames.hasNext()) {
        final String serverName = serverNames.next().asText();
        if (serversNode.has(serverName) && ApiTool.hasNode(serversNode.get(serverName), "security")) {
          ApiTool.getNode(serversNode.get(serverName), "security").forEach(requirementsArray::add);
        }
      }
    } else if (ApiTool.hasNode(root, "servers")) {
      final Iterator<Map.Entry<String, JsonNode>> serverEntries = ApiTool.getNode(root, "servers").fields();
      while (serverEntries.hasNext()) {
        final Map.Entry<String, JsonNode> serverEntry = serverEntries.next();
        if (ApiTool.hasNode(serverEntry.getValue(), "security")) {
          ApiTool.getNode(serverEntry.getValue(), "security").forEach(requirementsArray::add);
        }
      }
    }

    final var usedSchemes = new java.util.HashSet<String>();
    requirementsArray.forEach(reqObj -> reqObj.fieldNames().forEachRemaining(usedSchemes::add));

    ObjectNode collectedSchemes = null;
    if (ApiTool.hasNode(root, "components") && ApiTool.hasNode(ApiTool.getNode(root, "components"), "securitySchemes")) {
      final JsonNode schemes = ApiTool.getNode(ApiTool.getNode(root, "components"), "securitySchemes");
      final ObjectNode collectedMutable = MAPPER.createObjectNode();
      usedSchemes.stream()
                 .filter(schemes::has)
                 .forEach(name -> collectedMutable.set(name, schemes.get(name)));
      collectedSchemes = collectedMutable;
    }

    final String requirements = requirementsArray.size() > 0 ? requirementsArray.toString() : null;
    final String schemesStr = collectedSchemes != null && collectedSchemes.fieldNames().hasNext() ? collectedSchemes.toString() : null;
    return Pair.of(requirements, schemesStr);
  }

  protected Pair<String, String> deriveKafkaSecurity(final String securitySchemesJson) {
    if (StringUtils.isBlank(securitySchemesJson)) {
      return Pair.of(null, null);
    }
    try {
      final JsonNode schemes = MAPPER.readTree(securitySchemesJson);
      final var fieldNames = schemes.fieldNames();
      while (fieldNames.hasNext()) {
        final String name = fieldNames.next();
        final JsonNode scheme = schemes.get(name);
        final String type = ApiTool.getNodeAsString(scheme, "type");
        if (StringUtils.equalsAnyIgnoreCase(type, "plain", "userPassword")) {
          return Pair.of("PLAIN", "SASL_PLAINTEXT");
        } else if (StringUtils.equalsIgnoreCase(type, "scramSha256")) {
          return Pair.of("SCRAM-SHA-256", "SASL_PLAINTEXT");
        } else if (StringUtils.equalsIgnoreCase(type, "scramSha512")) {
          return Pair.of("SCRAM-SHA-512", "SASL_PLAINTEXT");
        } else if (StringUtils.equalsIgnoreCase(type, "xoauth2") || StringUtils.equalsIgnoreCase(type, "oauth2")) {
          return Pair.of("OAUTHBEARER", "SASL_PLAINTEXT");
        }
      }
    } catch (IOException e) {
      log.warn("Could not parse securitySchemes to derive Kafka security", e);
    }
    return Pair.of(null, null);
  }

  protected JsonNode applyTraits(final JsonNode node) {
    return applyTraits(node, currentRoot);
  }

  protected JsonNode applyTraits(final JsonNode node, final JsonNode root) {
    if (node == null || !node.has("traits") || !node.get("traits").isArray()) {
      return node;
    }
    final ObjectNode merged = MAPPER.createObjectNode();
    merged.setAll((ObjectNode) node);
    for (JsonNode traitNode : node.get("traits")) {
      JsonNode traitContent = traitNode;
      if (traitNode.isObject() && traitNode.has(REF) && Objects.nonNull(root)) {
        traitContent = resolveRefNode(root, ApiTool.getRefValue(traitNode));
      }
      if (traitContent != null && traitContent.isObject()) {
        mergeIfAbsent(merged, (ObjectNode) applyTraits(traitContent, root));
      }
    }
    merged.remove("traits");
    return merged;
  }

  private JsonNode resolveRefNode(final JsonNode root, final String refValue) {
    if (StringUtils.isBlank(refValue) || Objects.isNull(root)) {
      return null;
    }
    if (refValue.startsWith("#/")) {
      final String[] segments = refValue.substring(2).split("/");
      JsonNode cursor = root;
      for (final String segment : segments) {
        if (cursor == null) {
          return null;
        }
        cursor = cursor.get(segment);
      }
      return cursor;
    }
    log.warn("Trait $ref '{}' points to external document; external trait resolution is not yet supported.", refValue);
    return null;
  }

  private void mergeIfAbsent(final ObjectNode target, final ObjectNode source) {
    source.fieldNames().forEachRemaining(field -> {
      if (!target.has(field)) {
        target.set(field, source.get(field));
      } else if (target.get(field).isObject() && source.get(field).isObject()) {
        mergeIfAbsent((ObjectNode) target.get(field), (ObjectNode) source.get(field));
      }
    });
  }

  protected String evaluatePackage(final OperationParameterObject operation) {
    final String evaluated;
    if (operation != null && operation.getApiPackage() != null) {
      evaluated = operation.getApiPackage();
    } else {
      evaluated = Objects.requireNonNullElse(groupId, DEFAULT_ASYNCAPI_API_PACKAGE);
    }
    return evaluated;
  }

  protected void checkClassPackageDuplicate(final String className, final String apiPackage) {
    if (className != null && processedClassnames.contains(className)
        && apiPackage != null && processedApiPackages.contains(apiPackage)
        && processedClassnames.lastIndexOf(className) == processedApiPackages.lastIndexOf(apiPackage)) {
      throw new DuplicateClassException(className, apiPackage);
    }
  }

  protected void addProcessedClassesAndPackagesToGlobalVariables(final String className, final String apiPackage, final String defaultClassName) {
    processedClassnames.add(className != null ? className : defaultClassName);
    processedApiPackages.add(apiPackage != null ? apiPackage : DEFAULT_ASYNCAPI_API_PACKAGE);
  }

  protected boolean shouldBuild(final JsonNode schemaToBuild) {
    boolean result = Boolean.FALSE;
    if (ApiTool.hasRef(schemaToBuild)) {
      if (!ApiTool.getRefValue(schemaToBuild).contains(AVSC)) {
        result = Boolean.TRUE;
      }
    } else {
      result = Boolean.TRUE;
    }
    return result;
  }

  protected void writeSchemaObject(final boolean usingLombok, final String modelPackageReceived, final String keyClassName, final SchemaObject schemaObject) {
    final var destinationPackage = StringUtils.defaultIfEmpty(modelPackageReceived, DEFAULT_ASYNCAPI_API_PACKAGE + SLASH + schemaObject.getParentPackage());
    templateFactory.addSchemaObject(modelPackageReceived, keyClassName, schemaObject, destinationPackage, usingLombok);
    templateFactory.checkRequiredOrCombinatorExists(schemaObject, usingLombok);
  }

  protected abstract JsonNode getChannelFromOperation(final JsonNode openApi, final JsonNode operation);

  protected abstract String getOperationId(final JsonNode operation);
}
