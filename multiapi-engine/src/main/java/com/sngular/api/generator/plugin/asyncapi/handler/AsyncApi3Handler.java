package com.sngular.api.generator.plugin.asyncapi.handler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sngular.api.generator.plugin.asyncapi.exception.ChannelNameException;
import com.sngular.api.generator.plugin.asyncapi.exception.DuplicatedOperationException;
import com.sngular.api.generator.plugin.asyncapi.exception.ExternalRefComponentNotFoundException;
import com.sngular.api.generator.plugin.asyncapi.exception.FileSystemException;
import com.sngular.api.generator.plugin.asyncapi.exception.InvalidAsyncAPIException;
import com.sngular.api.generator.plugin.asyncapi.exception.InvalidAvroException;
import com.sngular.api.generator.plugin.asyncapi.model.ProcessBindingsResult;
import com.sngular.api.generator.plugin.asyncapi.model.ProcessMethodResult;
import com.sngular.api.generator.plugin.asyncapi.parameter.OperationParameterObject;
import com.sngular.api.generator.plugin.asyncapi.parameter.SpecFile;
import com.sngular.api.generator.plugin.asyncapi.util.AsyncApiUtil;
import com.sngular.api.generator.plugin.asyncapi.util.BindingTypeEnum;
import com.sngular.api.generator.plugin.asyncapi.util.FactoryTypeEnum;
import com.sngular.api.generator.plugin.asyncapi.util.ReferenceProcessor;
import com.sngular.api.generator.plugin.common.files.FileLocation;
import com.sngular.api.generator.plugin.common.model.CommonSpecFile;
import com.sngular.api.generator.plugin.common.tools.ApiTool;
import com.sngular.api.generator.plugin.common.tools.MapperContentUtil;
import com.sngular.api.generator.plugin.common.tools.MapperUtil;
import com.sngular.api.generator.plugin.common.tools.SchemaUtil;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncApi3Handler extends BaseAsyncApiHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncApi3Handler.class);

  public AsyncApi3Handler(
      final Integer springBootVersion,
      boolean overwriteModel,
      final File targetFolder,
      final String processedGeneratedSourcesFolder,
      final String groupId,
      final File baseDir) {
    super(springBootVersion, overwriteModel, targetFolder, processedGeneratedSourcesFolder, groupId, baseDir);
  }

  @Override
  public void processFileSpec(final List<SpecFile> specsListFile) {
    processedOperationIds.clear();
    templateFactory.setNotGenerateTemplate();
    for (final SpecFile fileParameter : specsListFile) {
      final FileLocation ymlParent;
      try {
        ymlParent = resolveYmlLocation(fileParameter.getFilePath());
      } catch (final IOException | URISyntaxException e) {
        throw new FileSystemException(e.getMessage());
      }
      try {
        final JsonNode openApi = AsyncApiUtil.getPojoFromSpecFile(baseDir, fileParameter);
        final JsonNode operations = openApi.get("operations");
        final Map<String, JsonNode> totalSchemas = getAllSchemas(ymlParent, openApi);
        final Iterator<Entry<String, JsonNode>> operationsIt = operations.fields();
        setUpTemplate(fileParameter, springBootVersion);
        while (operationsIt.hasNext()) {
          final Map.Entry<String, JsonNode> entry = operationsIt.next();
          final JsonNode operation = entry.getValue();
          final String operationId = entry.getKey();
          final JsonNode channel = getChannelFromOperation(openApi, operation);
          processOperation(fileParameter, ymlParent, entry, channel, operationId, operation, totalSchemas, openApi);
        }
        templateFactory.fillTemplates();
      } catch (final TemplateException | IOException e) {
        throw new FileSystemException(e);
      }
      templateFactory.clearData();
    }
  }

  @Override
  protected Map<String, JsonNode> getAllSchemas(final FileLocation ymlParent, final JsonNode node) {
    final Map<String, JsonNode> totalSchemas = new HashMap<>();
    final List<JsonNode> referenceList = node.findValues(REF);

    referenceList.forEach(reference -> {
      final ReferenceProcessor refProcessor = ReferenceProcessor.builder().ymlParent(ymlParent).totalSchemas(totalSchemas).build();
      refProcessor.processReference(node, ApiTool.getNodeAsString(reference));
    });

    ApiTool.getComponent(node, SCHEMAS).forEachRemaining(
        schema -> totalSchemas.putIfAbsent(SCHEMAS.toUpperCase() + SLASH + MapperUtil.getSchemaKey(schema.getKey()), schema.getValue())
                                                        );

    ApiTool.getComponent(node, MESSAGES).forEachRemaining(
        message -> getMessageSchemas(message.getKey(), message.getValue(), ymlParent, totalSchemas)
                                                         );

    getChannels(node).forEachRemaining(
        channel -> getChannelSchemas(channel.getValue(), totalSchemas, ymlParent)
                                      );

    return totalSchemas;
  }

  @Override
  protected void processOperation(
      final SpecFile fileParameter, final FileLocation ymlParent, final Entry<String, JsonNode> entry, final JsonNode channel,
      final String operationId, final JsonNode operation, final Map<String, JsonNode> totalSchemas, final JsonNode root) throws IOException, TemplateException {
    this.currentRoot = root;
    final String action = ApiTool.getNodeAsString(operation, "action");
    if (!StringUtils.endsWithIgnoreCase(action, "send") && !StringUtils.endsWithIgnoreCase(action, "receive")) {
      throw new InvalidAsyncAPIException("Operation action must be either 'send' or 'receive'");
    }

    if (isValidOperation(fileParameter.getConsumer(), operationId, action, "receive", true)) {
      final var operationObject = fileParameter.getConsumer();
      operationObject.setFilePath(fileParameter.getFilePath());
      checkClassPackageDuplicate(operationObject.getClassNamePostfix(), operationObject.getApiPackage());
      final String channelName = extractChannelName(operation);
      processSubscribeMethod(operationId, operation, operationObject, ymlParent, totalSchemas, channel, root, channelName);
      addProcessedClassesAndPackagesToGlobalVariables(operationObject.getClassNamePostfix(), operationObject.getApiPackage(), CONSUMER_CLASS_NAME);
    } else if (isValidOperation(fileParameter.getSupplier(), operationId, action, "send", Objects.isNull(fileParameter.getStreamBridge()))) {
      final var operationObject = fileParameter.getSupplier();
      operationObject.setFilePath(fileParameter.getFilePath());
      checkClassPackageDuplicate(operationObject.getClassNamePostfix(), operationObject.getApiPackage());
      final String channelName = extractChannelName(operation);
      processSupplierMethod(operationId, operation, operationObject, ymlParent, totalSchemas, channel, root, channelName);
      addProcessedClassesAndPackagesToGlobalVariables(operationObject.getClassNamePostfix(), operationObject.getApiPackage(), SUPPLIER_CLASS_NAME);
    } else if (isValidOperation(fileParameter.getStreamBridge(), operationId, action, "send", Objects.isNull(fileParameter.getSupplier()))) {
      final var operationObject = fileParameter.getStreamBridge();
      operationObject.setFilePath(fileParameter.getFilePath());
      checkClassPackageDuplicate(operationObject.getClassNamePostfix(), operationObject.getApiPackage());
      final String channelName = extractChannelName(operation);
      processStreamBridgeMethod(operationId, operation, operationObject, ymlParent, StringUtils.defaultIfBlank(channelName, entry.getKey()), totalSchemas, channel, root);
      addProcessedClassesAndPackagesToGlobalVariables(operationObject.getClassNamePostfix(), operationObject.getApiPackage(), STREAM_BRIDGE_CLASS_NAME);
    }
  }

  @Override
  protected void processSupplierMethod(
      final String operationId, final JsonNode operation, final OperationParameterObject operationObject, final FileLocation ymlParent,
      final Map<String, JsonNode> totalSchemas, final JsonNode channel, final JsonNode root, final String channelName) throws IOException {
    final JsonNode opWithTraits = applyTraits(operation);
    final JsonNode channelWithTraits = applyTraits(channel);
    final String channelBindings = stringify(ApiTool.getNode(channelWithTraits, BINDINGS));
    final String operationBindings = stringify(ApiTool.getNode(opWithTraits, BINDINGS));
    final String serverBindings = extractServerBindings(root, operation);
    final var security = extractSecurity(root, operation);
    final String channelParameters = stringify(ApiTool.getNode(channelWithTraits, "parameters"));
    final ProcessMethodResult result = processMethod(operationId, opWithTraits, operationObject, ymlParent, totalSchemas, channelBindings, operationBindings, serverBindings,
                                                     security.getLeft(), security.getRight(), channelParameters);
    result.setChannelName(channelName);
    if (StringUtils.isBlank(result.getReplyTo()) && StringUtils.isNotBlank(channelName)) {
      result.setReplyTo(channelName + "-reply");
    }
    fillTemplateFactory(operationId, result, totalSchemas, operationObject);
    templateFactory.addSupplierMethod(result.getOperationId(), result.getNamespace(), result.getChannelName(), result.getBindings(), result.getBindingType(), result.getAction(),
                                      result.getServerBindings(), result.getChannelBindings(), result.getOperationBindings(), result.getMessageBindings(),
                                      result.getSecurityRequirements(), result.getSecuritySchemes(), result.getChannelParameters(),
                                      result.getCorrelationId(), result.getCausationId(), result.getReplyTo(), result.getBindingVersion(), result.getMqttQos(),
                                      result.getMqttRetain(), result.getWebsocketMethod(), result.getWebsocketSubprotocol(), result.getWebsocketHeaders(),
                                      result.getKafkaSaslMechanism(), result.getKafkaSecurityProtocol(), result.getKafkaPartition(), result.getKafkaHeaders(),
                                      result.getKafkaTopicConfiguration(), result.getSchemaFormat(), result.getSchemaVersion(), result.getExamples(), result.getCloudEvent(),
                                      result.getKeySelector());
  }

  @Override
  protected void processStreamBridgeMethod(
      final String operationId, final JsonNode operation, final OperationParameterObject operationObject, final FileLocation ymlParent, final String channelName,
      final Map<String, JsonNode> totalSchemas, final JsonNode channel, final JsonNode root)
      throws IOException {
    final JsonNode opWithTraits = applyTraits(operation);
    final JsonNode channelWithTraits = applyTraits(channel);
    final String channelBindings = stringify(ApiTool.getNode(channelWithTraits, BINDINGS));
    final String operationBindings = stringify(ApiTool.getNode(opWithTraits, BINDINGS));
    final String serverBindings = extractServerBindings(root, operation);
    final var security = extractSecurity(root, operation);
    final String channelParameters = stringify(ApiTool.getNode(channelWithTraits, "parameters"));
    final ProcessMethodResult result = processMethod(operationId, opWithTraits, operationObject, ymlParent, totalSchemas, channelBindings, operationBindings, serverBindings,
                                                     security.getLeft(), security.getRight(), channelParameters);
    result.setChannelName(channelName);
    if (StringUtils.isBlank(result.getReplyTo()) && StringUtils.isNotBlank(channelName)) {
      result.setReplyTo(channelName + "-reply");
    }
    if (!channelName.matches("[a-zA-Z0-9._/#{}:+\\-]*")) {
      throw new ChannelNameException(channelName);
    }
    fillTemplateFactory(operationId, result, totalSchemas, operationObject);
    templateFactory.addStreamBridgeMethod(result.getOperationId(), result.getNamespace(), channelName, result.getBindings(), result.getBindingType(), result.getAction(),
                                          result.getServerBindings(), result.getChannelBindings(), result.getOperationBindings(), result.getMessageBindings(),
                                          result.getSecurityRequirements(), result.getSecuritySchemes(), result.getChannelParameters(),
                                          result.getCorrelationId(), result.getCausationId(), result.getReplyTo(), result.getBindingVersion(), result.getMqttQos(),
                                          result.getMqttRetain(), result.getWebsocketMethod(), result.getWebsocketSubprotocol(), result.getWebsocketHeaders(),
                                          result.getKafkaSaslMechanism(), result.getKafkaSecurityProtocol(), result.getKafkaPartition(), result.getKafkaHeaders(),
                                          result.getKafkaTopicConfiguration(), result.getSchemaFormat(), result.getSchemaVersion(), result.getExamples(), result.getCloudEvent(),
                                          result.getKeySelector());
  }

  @Override
  protected void processSubscribeMethod(
      final String operationId,
      final JsonNode operation, final OperationParameterObject operationObject, final FileLocation ymlParent,
      final Map<String, JsonNode> totalSchemas, final JsonNode channel, final JsonNode root, final String channelName) throws IOException {
    final JsonNode opWithTraits = applyTraits(operation);
    final JsonNode channelWithTraits = applyTraits(channel);
    final String channelBindings = stringify(ApiTool.getNode(channelWithTraits, BINDINGS));
    final String operationBindings = stringify(ApiTool.getNode(opWithTraits, BINDINGS));
    final String serverBindings = extractServerBindings(root, operation);
    final var security = extractSecurity(root, operation);
    final String channelParameters = stringify(ApiTool.getNode(channelWithTraits, "parameters"));
    final ProcessMethodResult result = processMethod(operationId, opWithTraits, operationObject, ymlParent, totalSchemas, channelBindings, operationBindings, serverBindings,
                                                     security.getLeft(), security.getRight(), channelParameters);
    result.setChannelName(channelName);
    if (StringUtils.isBlank(result.getReplyTo()) && StringUtils.isNotBlank(channelName)) {
      result.setReplyTo(channelName + "-reply");
    }
    fillTemplateFactory(operationId, result, totalSchemas, operationObject);
    templateFactory.addSubscribeMethod(result.getOperationId(), result.getNamespace(), result.getChannelName(), result.getBindings(), result.getBindingType(), result.getAction(),
                                       result.getServerBindings(), result.getChannelBindings(), result.getOperationBindings(), result.getMessageBindings(),
                                       result.getSecurityRequirements(), result.getSecuritySchemes(), result.getChannelParameters(),
                                       result.getCorrelationId(), result.getCausationId(), result.getReplyTo(), result.getBindingVersion(), result.getMqttQos(),
                                       result.getMqttRetain(), result.getWebsocketMethod(), result.getWebsocketSubprotocol(), result.getWebsocketHeaders(),
                                       result.getKafkaSaslMechanism(), result.getKafkaSecurityProtocol(), result.getKafkaPartition(), result.getKafkaHeaders(),
                                       result.getKafkaTopicConfiguration(), result.getSchemaFormat(), result.getSchemaVersion(), result.getExamples(), result.getCloudEvent(),
                                       result.getKeySelector());
  }

  @Override
  protected void fillTemplateFactory(
      final String operationId,
      final ProcessMethodResult processedMethod, final Map<String, JsonNode> totalSchemas, final OperationParameterObject operationObject)
      throws IOException {
    final String classFullName = processedMethod.getNamespace();
    final String keyClassFullName = processedMethod.getBindings();
    final String modelPackage = classFullName.substring(0, classFullName.lastIndexOf("."));
    final String parentPackage = modelPackage.substring(modelPackage.lastIndexOf(".") + 1);
    final String className = classFullName.substring(classFullName.lastIndexOf(".") + 1);
    String className = classFullName.substring(classFullName.lastIndexOf(".") + 1);
    String suffix = operationObject.getModelNameSuffix();
    if (StringUtils.isNotBlank(className)) {
      className = className.substring(0, 1).toUpperCase() + (className.length() > 1 ? className.substring(1) : "");
    }
    if (StringUtils.isNotBlank(suffix) && !className.endsWith(suffix)) {
      className = className + suffix;
    }
    final String keyClassName = keyClassFullName != null ? keyClassFullName.substring(keyClassFullName.lastIndexOf(".") + 1) : null;
    final JsonNode schemaToBuild = processedMethod.getPayload();
    if (shouldBuild(schemaToBuild)) {
      final var schemaObjectIt =
          MapperContentUtil.mapComponentToSchemaObject(totalSchemas, className, schemaToBuild, parentPackage, operationObject, this.baseDir).iterator();

      if (schemaObjectIt.hasNext()) {
        writeSchemaObject(operationObject.isUseLombokModelAnnotation(), operationObject.getModelPackage(), keyClassName, schemaObjectIt.next());
        if (Objects.nonNull(keyClassName)) {
          templateFactory.setWrapperPackageName(operationObject.getApiPackage());
          templateFactory.fillTemplateWrapper(operationObject.getApiPackage(), classFullName, className, keyClassFullName, keyClassName);
        }
        schemaObjectIt.forEachRemaining(schemaObj -> writeSchemaObject(operationObject.isUseLombokModelAnnotation(), operationObject.getModelPackage(), null, schemaObj));
      }
    }
  }

  @Override
  protected ProcessMethodResult processMethod(
      final String operationId, final JsonNode operation, final OperationParameterObject operationObject, final FileLocation ymlParent, final Map<String, JsonNode> totalSchemas,
      final String channelBindings, final String operationBindings, final String serverBindings, final String securityRequirements, final String securitySchemes,
      final String channelParameters)
      throws IOException {
    final String action = ApiTool.getNodeAsString(operation, "action");
    final JsonNode opWithTraits = applyTraits(operation);
    final JsonNode message = opWithTraits.get("messages").get(0);
    final JsonNode messageWithTraits = applyTraits(message);
    final Pair<String, JsonNode> payloadInfo;
    final var processBindingsResultBuilder = ProcessBindingsResult.builder()
                                                                  .channelBindings(channelBindings)
                                                                  .operationBindings(operationBindings)
                                                                  .serverBindings(serverBindings)
                                                                  .securityRequirements(securityRequirements)
                                                                  .securitySchemes(securitySchemes);
    if (messageWithTraits.has(REF)) {
      payloadInfo = processMethodRef(processBindingsResultBuilder, ApiTool.getRefValue(messageWithTraits), operationObject, ymlParent, totalSchemas,
                                     messageWithTraits);
    } else if (messageWithTraits.has(PAYLOAD)) {
      payloadInfo = processPayload(operationObject, calculateMessageName(operationId, messageWithTraits), ApiTool.getNode(messageWithTraits, PAYLOAD), ymlParent);
      if (ApiTool.hasNode(messageWithTraits, BINDINGS)) {
        processBindings(processBindingsResultBuilder, messageWithTraits, operationObject);
      }
    } else {
      throw new InvalidAsyncAPIException(operationId);
    }
    final var processBindingsResult = processBindingsResultBuilder.build();
    final String correlationId = stringify(ApiTool.getNode(messageWithTraits, "correlationId"));
    final String replyTo = ApiTool.getNodeAsString(messageWithTraits, "replyTo");
    final String causationId = ApiTool.getNodeAsString(messageWithTraits, "causationId");
    final var kafkaSecurity = deriveKafkaSecurity(processBindingsResult.getSecuritySchemes());
    final String rawSchemaFormat = StringUtils.defaultIfBlank(ApiTool.getNodeAsString(messageWithTraits, "schemaFormat"), ApiTool.getNodeAsString(payloadInfo.getValue(), "schemaFormat"));
    final String schemaFormat = selectSchemaPipeline(rawSchemaFormat);
    final String schemaVersion = StringUtils.defaultIfBlank(ApiTool.getNodeAsString(messageWithTraits, "schemaVersion"), ApiTool.getNodeAsString(payloadInfo.getValue(), "schemaVersion"));
    final boolean cloudEvent = isCloudEventPayload(payloadInfo.getValue());
    final String versionedNamespace = applySchemaVersionNamespace(payloadInfo.getKey(), schemaVersion);
    return ProcessMethodResult
               .builder()
               .operationId(operationId)
               .namespace(versionedNamespace)
               .payload(payloadInfo.getValue())
               .bindings(processBindingsResult.getBindings())
               .bindingType(processBindingsResult.getBindingType())
               .action(action)
               .serverBindings(processBindingsResult.getServerBindings())
               .channelBindings(processBindingsResult.getChannelBindings())
               .operationBindings(processBindingsResult.getOperationBindings())
               .messageBindings(processBindingsResult.getMessageBindings())
               .securityRequirements(processBindingsResult.getSecurityRequirements())
               .securitySchemes(processBindingsResult.getSecuritySchemes())
               .channelParameters(channelParameters)
               .bindingVersion(processBindingsResult.getBindingVersion())
               .mqttQos(processBindingsResult.getMqttQos())
               .mqttRetain(processBindingsResult.getMqttRetain())
               .websocketMethod(processBindingsResult.getWebsocketMethod())
               .websocketSubprotocol(processBindingsResult.getWebsocketSubprotocol())
               .websocketHeaders(processBindingsResult.getWebsocketHeaders())
               .kafkaSaslMechanism(kafkaSecurity.getLeft())
               .kafkaSecurityProtocol(kafkaSecurity.getRight())
               .kafkaPartition(processBindingsResult.getKafkaPartition())
               .kafkaHeaders(processBindingsResult.getKafkaHeaders())
               .kafkaTopicConfiguration(processBindingsResult.getKafkaTopicConfiguration())
               .schemaFormat(schemaFormat)
               .schemaVersion(schemaVersion)
               .correlationId(correlationId)
               .causationId(causationId)
               .replyTo(replyTo)
               .examples(processBindingsResult.getExamples())
               .keySelector(processBindingsResult.getKeySelector())
               .cloudEvent(cloudEvent)
               .build();
  }

  private boolean isCloudEventPayload(final JsonNode payload) {
    if (payload == null || !payload.has("properties")) {
      return false;
    }
    final JsonNode props = payload.get("properties");
    return props.has("specversion") && props.has("id");
  }

  @Override
  protected Pair<String, JsonNode> processPayload(final OperationParameterObject operationObject, final String messageName, final JsonNode payload, final FileLocation ymlParent)
      throws IOException {
    final String namespace;
    String className = messageName;
    String suffix = operationObject.getModelNameSuffix();
    if (StringUtils.isNotBlank(suffix) && !messageName.endsWith(suffix)) {
      className = messageName + suffix;
    }
    if (ApiTool.hasRef(payload)) {
      namespace = processMessageRef(payload, operationObject.getModelPackage(), ymlParent);
    } else {
      namespace = operationObject.getModelPackage() + PACKAGE_SEPARATOR_STR + className;
    }
    return Pair.of(namespace, payload);
  }

  @Override
  protected Pair<String, JsonNode> processMethodRef(
      final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final String messageRef, final OperationParameterObject operationObject,
      final FileLocation ymlParent, final Map<String, JsonNode> totalSchemas, final JsonNode method) throws IOException {

    final var message = totalSchemas.get(MapperUtil.getRefSchemaKey(messageRef));
    if (ApiTool.hasNode(message, BINDINGS)) {
      processBindings(bindingsResult, message, operationObject);
    }
    String refClassName = MapperUtil.getRefClass(method);
    String suffix = operationObject.getModelNameSuffix();
    if (StringUtils.isNotBlank(suffix) && !messageName.endsWith(suffix)) {
      refClassName = refClassName + suffix;
    }
    return processPayload(operationObject, refClassName, solvePayload(message, totalSchemas, ymlParent), ymlParent);
  }

  @Override
  protected String processMessageRef(final JsonNode messageBody, final String modelPackage, final FileLocation ymlParent) throws IOException {
    final String namespace;
    final String messageContent = ApiTool.getRefValue(messageBody);
    if (messageContent.startsWith("#")) {
      namespace = processModelPackage(MapperUtil.getLongRefClass(messageBody), modelPackage);
    } else if (messageContent.contains("#") || StringUtils.endsWith(messageContent, "yml")
               || StringUtils.endsWith(messageContent, "yaml") || StringUtils.endsWith(messageContent, "json")) {
      namespace = processExternalRef(modelPackage, ymlParent, messageBody);
    } else {
      namespace = processExternalAvro(ymlParent, messageContent);
    }
    return namespace;
  }

  @Override
  protected String processExternalAvro(final FileLocation ymlParent, final String messageContent) throws IOException {
    String avroFilePath = messageContent;
    final String namespace;
    if (messageContent.startsWith(SLASH)) {
      avroFilePath = avroFilePath.replaceFirst(SLASH, "");
    } else if (messageContent.startsWith(".")) {
      avroFilePath = baseDir.toAbsolutePath() + avroFilePath.replaceFirst("\\.", "");
    }
    final InputStream avroFile = ymlParent.getFileAtLocation(avroFilePath);
    final ObjectMapper mapper = new ObjectMapper();
    try {
      final JsonNode fileTree = mapper.readTree(avroFile);
      final JsonNode avroNamespace = fileTree.get("namespace");

      if (avroNamespace == null) {
        throw new InvalidAvroException(avroFilePath);
      }

      namespace = avroNamespace.asText() + PACKAGE_SEPARATOR + fileTree.get("name").asText();
    } catch (final IOException e) {
      throw new FileSystemException(e);
    }
    return namespace;
  }

  @Override
  protected String processExternalRef(final String modelPackage, final FileLocation ymlParent, final JsonNode message) throws IOException {
    final String[] pathToFile = MapperUtil.splitReference(ApiTool.getRefValue(message));
    final String filePath = pathToFile[0];
    final JsonNode node = ApiTool.nodeFromFile(ymlParent, filePath, FactoryTypeEnum.YML);
    if (pathToFile.length > 1 && StringUtils.isNotEmpty(pathToFile[1])) {
      final String componentPath = pathToFile[1];

      if (node.at(MapperUtil.getPathToModel(componentPath)).has(MapperUtil.getModel(componentPath))) {
        return processModelPackage(MapperUtil.getModel(componentPath), modelPackage);
      } else {
        throw new ExternalRefComponentNotFoundException(MapperUtil.getModel(componentPath), filePath);
      }
    } else {
      return processModelPackage(MapperUtil.getNameFromFile(filePath), modelPackage);
    }
  }

  @Override
  protected void processBindings(
      final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final JsonNode message,
      final CommonSpecFile commonSpecFile) {
    if (message.has(BINDINGS)) {
      final var bindingsNode = message.get(BINDINGS);
      bindingsResult.messageBindings(stringify(bindingsNode));
      if (bindingsNode.has(KAFKA)) {
        processKafkaBindings(bindingsResult, bindingsNode.get(KAFKA), commonSpecFile);
      } else if (bindingsNode.has("mqtt")) {
        processMqttBindings(bindingsResult, bindingsNode.get("mqtt"));
      } else if (bindingsNode.has("ws")) {
        processWebsocketBindings(bindingsResult, bindingsNode.get("ws"));
      } else if (bindingsNode.has("websockets")) {
        processWebsocketBindings(bindingsResult, bindingsNode.get("websockets"));
      } else {
        bindingsResult.bindingType(BindingTypeEnum.NONBINDING.getValue());
      }
    }
  }

  @Override
  protected void processKafkaBindings(final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final JsonNode kafkaBindings, final CommonSpecFile specFile) {
    bindingsResult.bindingType(BindingTypeEnum.KAFKA.getValue());
    if (kafkaBindings.has("bindingVersion")) {
      bindingsResult.bindingVersion(ApiTool.getNodeAsString(kafkaBindings, "bindingVersion"));
      validateKafkaBindingVersion(ApiTool.getNodeAsString(kafkaBindings, "bindingVersion"));
    }
    if (kafkaBindings.has("acks")) {
      // map as qos equivalent for kafka
      bindingsResult.mqttQos(ApiTool.getNode(kafkaBindings, "acks").asInt());
    }
    if (kafkaBindings.has("partitions")) {
      bindingsResult.kafkaPartition(extractPartitionId(kafkaBindings.get("partitions")));
      bindingsResult.kafkaTopicConfiguration(stringify(ApiTool.getNode(kafkaBindings, "partitions")));
    }
    if (kafkaBindings.has("headers")) {
      bindingsResult.kafkaHeaders(stringify(ApiTool.getNode(kafkaBindings, "headers")));
    }
    if (kafkaBindings.has("topicConfiguration")) {
      bindingsResult.kafkaTopicConfiguration(stringify(ApiTool.getNode(kafkaBindings, "topicConfiguration")));
    }
    if (kafkaBindings.has(KEY)) {
      bindingsResult.bindings(MapperUtil.getSimpleType(ApiTool.getNode(kafkaBindings, KEY), specFile));
    }
    if (kafkaBindings.has(KEY) && ApiTool.getNode(kafkaBindings, KEY).has("examples")) {
      bindingsResult.examples(stringify(ApiTool.getNode(ApiTool.getNode(kafkaBindings, KEY), "examples")));
    }
    if (kafkaBindings.has("x-keySelector")) {
      bindingsResult.keySelector(ApiTool.getNodeAsString(kafkaBindings, "x-keySelector"));
    }
  }

  private void processMqttBindings(final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final JsonNode mqttBindings) {
    bindingsResult.bindingType(BindingTypeEnum.NONBINDING.getValue());
    if (mqttBindings.has("bindingVersion")) {
      bindingsResult.bindingVersion(ApiTool.getNodeAsString(mqttBindings, "bindingVersion"));
    }
    if (mqttBindings.has("qos")) {
      bindingsResult.mqttQos(ApiTool.getNode(mqttBindings, "qos").asInt());
    }
    if (mqttBindings.has("retain")) {
      bindingsResult.mqttRetain(ApiTool.getNode(mqttBindings, "retain").asBoolean());
    }
  }

  private void processWebsocketBindings(final ProcessBindingsResult.ProcessBindingsResultBuilder bindingsResult, final JsonNode wsBindings) {
    bindingsResult.bindingType(BindingTypeEnum.NONBINDING.getValue());
    if (wsBindings.has("bindingVersion")) {
      bindingsResult.bindingVersion(ApiTool.getNodeAsString(wsBindings, "bindingVersion"));
    }
    if (wsBindings.has("method")) {
      bindingsResult.websocketMethod(ApiTool.getNodeAsString(wsBindings, "method"));
    }
    if (wsBindings.has("subprotocol")) {
      bindingsResult.websocketSubprotocol(ApiTool.getNodeAsString(wsBindings, "subprotocol"));
    }
    if (wsBindings.has("headers")) {
      bindingsResult.websocketHeaders(stringify(ApiTool.getNode(wsBindings, "headers")));
    }
  }

  @Override
  protected String processModelPackage(final String extractedPackage, final String modelPackage) {
    final String processedPackage;
    if (modelPackage != null) {
      if (extractedPackage.contains(PACKAGE_SEPARATOR_STR) || extractedPackage.contains(SLASH)) {
        final var splitPackage = MapperUtil.splitName(extractedPackage);
        final var className = splitPackage[splitPackage.length - 1];
        processedPackage = modelPackage + PACKAGE_SEPARATOR_STR + StringUtils.capitalize(className);
      } else {
        processedPackage = modelPackage + MapperUtil.capitalizeWithPrefix(extractedPackage);
      }
    } else if (extractedPackage.contains(PACKAGE_SEPARATOR_STR)) {
      final var splitPackage = MapperUtil.splitReference(extractedPackage);
      final var className = splitPackage[splitPackage.length - 1];
      processedPackage =
          StringUtils.join(PACKAGE_SEPARATOR_STR, Arrays.spliterator(splitPackage, 0, splitPackage.length)) + PACKAGE_SEPARATOR_STR + StringUtils.capitalize(className);
    } else {
      processedPackage = DEFAULT_ASYNCAPI_MODEL_PACKAGE + MapperUtil.capitalizeWithPrefix(extractedPackage);
    }

    return processedPackage;
  }

  @Override
  protected JsonNode getChannelFromOperation(final JsonNode openApi, final JsonNode operation) {
    if (operation.has("channel")) {
      final JsonNode channelRef = operation.get("channel");
      if (channelRef.has(REF)) {
        final String channelPath = ApiTool.getRefValue(channelRef);
        return ApiTool.getNode(openApi, channelPath.substring(1));
      }
    }
    throw new InvalidAsyncAPIException("Operation must have a channel reference");
  }

  private String selectSchemaPipeline(final String rawSchemaFormat) {
    if (StringUtils.isBlank(rawSchemaFormat)) {
      return "json";
    }
    final String lowerFormat = rawSchemaFormat.toLowerCase(Locale.ROOT);
    if (StringUtils.contains(lowerFormat, "avro")) {
      return "avro";
    }
    if (StringUtils.contains(lowerFormat, "protobuf") || StringUtils.contains(lowerFormat, "proto")) {
      throw new InvalidAsyncAPIException("schemaFormat 'protobuf' detected but protobuf pipeline is not implemented yet");
    }
    if (StringUtils.contains(lowerFormat, "json")) {
      return "json";
    }
    LOGGER.warn("Unsupported schemaFormat '{}' detected; using default JSON Schema pipeline", rawSchemaFormat);
    return "json";
  }

  private String extractChannelName(final JsonNode operation) {
    if (operation.has("channel")) {
      final JsonNode channelRef = operation.get("channel");
      if (channelRef.isTextual()) {
        return channelRef.asText();
      }
      if (channelRef.has(REF)) {
        final String refValue = ApiTool.getRefValue(channelRef);
        return StringUtils.removeStart(refValue, "#/channels/");
      }
    }
    return null;
  }

  @Override
  protected String getOperationId(final JsonNode operation) {
    if (!operation.has(OPERATION_ID)) {
      throw new InvalidAsyncAPIException("Operation must have an operationId");
    }
    final String operationId = operation.get(OPERATION_ID).asText();
    if (processedOperationIds.contains(operationId)) {
      throw new DuplicatedOperationException(operationId);
    }
    processedOperationIds.add(operationId);
    return operationId;
  }

  private JsonNode solvePayload(final JsonNode message, Map<String, JsonNode> totalSchemas, FileLocation ymlParent) {
    JsonNode payloadNode;
    if (ApiTool.hasNode(message, PAYLOAD)) {
      payloadNode = ApiTool.getNode(message, PAYLOAD);
    } else if (ApiTool.hasRef(message)) {
      final var solvedMessage = SchemaUtil.solveRef(ApiTool.getRefValue(message), totalSchemas, ymlParent.path());
      payloadNode = solvePayload(solvedMessage, totalSchemas, ymlParent);
    } else {
      payloadNode = message;
    }
    return payloadNode;
  }

  private Iterator<Entry<String, JsonNode>> getChannels(final JsonNode node) {
    return ApiTool.hasNode(node, CHANNELS) ? ApiTool.getFieldIterator(node, CHANNELS) : Collections.emptyIterator();
  }

  private void getMessageSchemas(
      final String messageName, final JsonNode message, final FileLocation ymlParent, final Map<String, JsonNode> totalSchemas) {
    if (ApiTool.hasNode(message, PAYLOAD)) {
      final JsonNode payload = message.get(PAYLOAD);
      if (!payload.has(REF)) {
        final String key = EVENT + SLASH + MapperUtil.getSchemaKey(calculateMessageName(messageName, message));
        totalSchemas.putIfAbsent(key, payload);
      }
    } else if (ApiTool.hasRef(message)) {
      final ReferenceProcessor refProcessor = ReferenceProcessor.builder().ymlParent(ymlParent).totalSchemas(totalSchemas).build();
      refProcessor.processReference(message, ApiTool.getRefValue(message));
    }
  }

  private String calculateMessageName(final String messageName, final JsonNode message) {
    final String finalMessageName;
    if (ApiTool.hasNode(message, "name")) {
      finalMessageName = ApiTool.getNodeAsString(message, "name");
    } else if (ApiTool.hasName(message)) {
      finalMessageName = ApiTool.getName(message);
    } else {
      finalMessageName = messageName;
    }
    return StringUtils.capitalize(finalMessageName);
  }

  private void getChannelSchemas(final JsonNode channel, final Map<String, JsonNode> totalSchemas, final FileLocation ymlParent) {
    if (ApiTool.hasNode(channel, MESSAGES)) {
      final var messagesIt = ApiTool.getFieldIterator(channel, MESSAGES);
      while (messagesIt.hasNext()) {
        final var message = messagesIt.next();
        getMessageSchemas(message.getKey(), message.getValue(), ymlParent, totalSchemas);
      }
    }
  }

  private boolean isValidOperation(
      final OperationParameterObject operation, final String operationId, final String action, final String expectedAction,
      final boolean excludingOperationExists) {
    final boolean result;
    if (operation != null) {
      final List<String> operationIds = operation.getOperationIds();
      result = operationIds.contains(operationId) || operationIds.isEmpty() && action.equals(expectedAction) && excludingOperationExists;
    } else {
      result = false;
    }
    return result;
  }
}
