/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.sngular.api.generator.plugin.asyncapi.template;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sngular.api.generator.plugin.asyncapi.exception.NonSupportedBindingException;
import com.sngular.api.generator.plugin.asyncapi.model.MethodObject;
import com.sngular.api.generator.plugin.asyncapi.parameter.SpecFile;
import com.sngular.api.generator.plugin.asyncapi.util.BindingTypeEnum;
import com.sngular.api.generator.plugin.common.template.CommonTemplateFactory;

import com.sngular.api.generator.plugin.asyncapi.util.NameUtils;

import org.apache.commons.lang3.StringUtils;

public class TemplateFactory extends CommonTemplateFactory {

  private static final String SUBSCRIBE_PACKAGE = "subscribePackage";
  private static final String WRAPPER_PACKAGE = "wrapperPackage";
  private static final String SUPPLIER_PACKAGE = "supplierPackage";
  private static final String STREAM_BRIDGE_PACKAGE = "streamBridgePackage";
  private static final String SUPPLIER_ENTITIES_SUFFIX = "supplierEntitiesSuffix";
  private static final String STREAM_BRIDGE_ENTITIES_SUFFIX = "streamBridgeEntitiesSuffix";
  private static final String SUBSCRIBE_ENTITIES_SUFFIX = "subscribeEntitiesSuffix";
  private static final String PUBLISH_METHODS = "publishMethods";
  private static final String SUBSCRIBE_METHODS = "subscribeMethods";
  private static final String STREAM_BRIDGE_METHODS = "streamBridgeMethods";
  private static final String CLASS_NAMESPACE = "classNamespace";
  private static final String CLASS_NAME = "className";
  private static final String KEY_NAMESPACE = "keyNamespace";
  private static final String KEY_CLASS_NAME = "keyClassName";
  private static final String METHOD = "method";
  private static final String SUBSCRIBE_CLASS_NAME = "subscribeClassName";
  private static final String SUPPLIER_CLASS_NAME = "supplierClassName";
  private static final String STREAM_BRIDGE_CLASS_NAME = "streamBridgeClassName";
  private static final String JAVA_EE_PACKAGE = "javaEEPackage";
  private static final String KAFKA_KEY_HEADER = "kafkaKeyHeader";
  private static final String SUPPLIER_SUFFIX = "Supplier";
  private static final String CONSUMER_SUFFIX = "Consumer";
  private static final String BRIDGE_SUFFIX = "Bridge";
  private static final String STREAM_BRIDGE_SUFFIX = "StreamBridge";
  private static final String WRAPPER_SUFFIX = "MessageWrapper";
  private static final String SCHEMA_REGISTRY_CLASS_NAME = "SchemaRegistryConfig";
  private static final String INTERFACE_PREFIX = "I";
  private static final String INTERFACE_SUFFIX = "Detailed";
  private static final String PUBLISH = "publish";
  private static final String SUBSCRIBE = "subscribe";
  private static final String STREAM_BRIDGE = "streamBridge";
  private static final String JAKARTA = "jakarta";
  private static final String JAVAX = "javax";
  private static final String KEY = "KEY";
  private static final String MESSAGE_KEY = "MESSAGE_KEY";

  private final List<MethodObject> publishMethods = new ArrayList<>();

  private final List<MethodObject> subscribeMethods = new ArrayList<>();

  private final List<MethodObject> streamBridgeMethods = new ArrayList<>();

  private String subscribeFilePath = null;

  private String supplierFilePath = null;

  private String streamBridgeFilePath = null;

  private String schemaRegistryFilePath = null;

  private String supplierClassName = null;

  private String streamBridgeClassName = null;

  private String subscribeClassName = null;

  public TemplateFactory(
      boolean enableOverwrite,
      final File targetFolder,
      final String processedGeneratedSourcesFolder,
      final File baseDir) {
    super(enableOverwrite, targetFolder, processedGeneratedSourcesFolder, baseDir, new ClasspathTemplateLoader());
  }

  public final void fillTemplates() throws IOException {
    addToRoot(PUBLISH_METHODS, publishMethods);
    addToRoot(SUBSCRIBE_METHODS, subscribeMethods);
    addToRoot(STREAM_BRIDGE_METHODS, streamBridgeMethods);

    applyFinalClassNaming();

    for (final var method : publishMethods) {
      String finalSupplierClassName = NameUtils.withOneSuffix(method.getClassName(), SUPPLIER_SUFFIX);
      fillTemplate(
          supplierFilePath,
          finalSupplierClassName,
          checkTemplate(method.getBindingType(), TemplateIndexConstants.TEMPLATE_API_SUPPLIERS));
    }

    for (final var method : subscribeMethods) {
      String finalSubscribeClassName = NameUtils.withOneSuffix(method.getClassName(), CONSUMER_SUFFIX);
      fillTemplate(
          subscribeFilePath,
          finalSubscribeClassName,
          checkTemplate(method.getBindingType(), TemplateIndexConstants.TEMPLATE_API_CONSUMERS));
    }

    for (final var method : streamBridgeMethods) {
      String finalStreamBridgeClassName = NameUtils.withOneSuffix(method.getClassName(), BRIDGE_SUFFIX);
      fillTemplate(
          streamBridgeFilePath,
          finalStreamBridgeClassName,
          checkTemplate(method.getBindingType(), TemplateIndexConstants.TEMPLATE_API_STREAM_BRIDGE));
    }

    if (schemaRegistryFilePath != null) {
      fillTemplate(
          schemaRegistryFilePath,
          SCHEMA_REGISTRY_CLASS_NAME,
          TemplateIndexConstants.TEMPLATE_SCHEMA_REGISTRY_CONFIG);
    }

    generateTemplates();
    generateInterfaces();
  }

  private String checkTemplate(final String bindingType, final String defaultTemplate) {
    final String templateName;
    switch (BindingTypeEnum.valueOf(bindingType)) {
      case NONBINDING:
        templateName = defaultTemplate;
        break;
      case KAFKA:
        templateName = StringUtils.remove(defaultTemplate, ".ftlh") 
                       + TemplateIndexConstants.KAFKA_BINDINGS_FTLH;
        break;
      default:
        throw new NonSupportedBindingException(bindingType);
    }
    return templateName;
  }

  private void generateInterfaces() throws IOException {
    final ArrayList<MethodObject> allMethods = new ArrayList<>(subscribeMethods);
    allMethods.addAll(publishMethods);

    for (MethodObject method : allMethods) {
      addToRoot(METHOD, method);

      String prefix = INTERFACE_PREFIX;
      String suffix = INTERFACE_SUFFIX;
      String interfaceName = NameUtils.withPrefixAndSuffix(prefix, method.getClassName(), suffix);

      if (PUBLISH.equals(method.getType())) {
        fillTemplate(supplierFilePath, interfaceName,
                    checkTemplate(method.getBindingType(), TemplateIndexConstants.TEMPLATE_INTERFACE_SUPPLIERS));
      } else if (SUBSCRIBE.equals(method.getType())) {
        fillTemplate(subscribeFilePath, interfaceName,
                    checkTemplate(method.getBindingType(), TemplateIndexConstants.TEMPLATE_INTERFACE_CONSUMERS));
      }
    }
    cleanData();
  }


  private void applyFinalClassNaming() {
    for (MethodObject method : publishMethods) {
      String base = method.getClassName();
      String finalName = NameUtils.withOneSuffix(base, SUPPLIER_SUFFIX);
      method.setClassName(finalName);
    }
    for (MethodObject method : subscribeMethods) {
      String base = method.getClassName();
      String finalName = NameUtils.withOneSuffix(base, CONSUMER_SUFFIX);
      method.setClassName(finalName);
    }
    for (MethodObject method : streamBridgeMethods) {
      String base = method.getClassName();
      String finalName = NameUtils.withOneSuffix(base, STREAM_BRIDGE_SUFFIX);
      method.setClassName(finalName);
    }
  }

  public final void setSubscribePackageName(final String packageName) {
    addToRoot(SUBSCRIBE_PACKAGE, packageName);
  }

  public final void setWrapperPackageName(final String packageName) {
    addToRoot(WRAPPER_PACKAGE, packageName);
  }

  public final void setSupplierPackageName(final String packageName) {
    addToRoot(SUPPLIER_PACKAGE, packageName);
  }

  public final void setStreamBridgePackageName(final String packageName) {
    addToRoot(STREAM_BRIDGE_PACKAGE, packageName);
  }

  public final void setSubscribeClassName(final String className) {
    addToRoot(SUBSCRIBE_CLASS_NAME, className);
    this.subscribeClassName = className;
  }

  public final void setSupplierClassName(final String className) {
    addToRoot(SUPPLIER_CLASS_NAME, className);
    this.supplierClassName = className;
  }

  public final void setStreamBridgeClassName(final String className) {
    addToRoot(STREAM_BRIDGE_CLASS_NAME, className);
    this.streamBridgeClassName = className;
  }

  public final void addSupplierMethod(
      final String operationId, final String classNamespace, final String channelName, final String bindings, final String bindingType, final String action,
      final String serverBindings,
      final String channelBindings, final String operationBindings, final String messageBindings, final String securityRequirements, final String securitySchemes,
      final String channelParameters, final String correlationId, final String causationId, final String replyTo, final String bindingVersion, final Integer mqttQos,
      final Boolean mqttRetain, final String websocketMethod, final String websocketSubprotocol, final String websocketHeaders, final String kafkaSaslMechanism,
      final String kafkaSecurityProtocol, final Integer kafkaPartition, final String kafkaHeaders, final String kafkaTopicConfiguration, final String schemaFormat,
      final String schemaVersion, final String examples, final Boolean cloudEvent, final String keySelector) {
    publishMethods.add(MethodObject
                           .builder()
                           .operationId(operationId)
                           .classNamespace(classNamespace)
                           .channelName(channelName)
                           .type(PUBLISH)
                           .action(action)
                           .keyClassNamespace(bindings)
                            .bindingType(bindingType)
                            .serverBindings(serverBindings)
                           .channelBindings(channelBindings)
                           .operationBindings(operationBindings)
                           .messageBindings(messageBindings)
                           .securityRequirements(securityRequirements)
                           .securitySchemes(securitySchemes)
                           .channelParameters(channelParameters)
                           .bindingVersion(bindingVersion)
                           .mqttQos(mqttQos)
                           .mqttRetain(mqttRetain)
                           .websocketMethod(websocketMethod)
                           .websocketSubprotocol(websocketSubprotocol)
                           .websocketHeaders(websocketHeaders)
                           .kafkaSaslMechanism(kafkaSaslMechanism)
                           .kafkaSecurityProtocol(kafkaSecurityProtocol)
                           .kafkaPartition(kafkaPartition)
                           .kafkaHeaders(kafkaHeaders)
                           .kafkaTopicConfiguration(kafkaTopicConfiguration)
                           .schemaFormat(schemaFormat)
                           .schemaVersion(schemaVersion)
                           .correlationId(correlationId)
                           .causationId(causationId)
                           .replyTo(replyTo)
                           .examples(examples)
                           .cloudEvent(cloudEvent)
                           .keySelector(keySelector)
                           .build());
  }

  public final void addStreamBridgeMethod(
      final String operationId, final String classNamespace, final String channelName, final String bindings, final String bindingType, final String action,
      final String serverBindings, final String channelBindings, final String operationBindings, final String messageBindings, final String securityRequirements,
      final String securitySchemes, final String channelParameters, final String correlationId, final String causationId, final String replyTo, final String bindingVersion,
      final Integer mqttQos, final Boolean mqttRetain, final String websocketMethod, final String websocketSubprotocol, final String websocketHeaders,
      final String kafkaSaslMechanism, final String kafkaSecurityProtocol, final Integer kafkaPartition, final String kafkaHeaders, final String kafkaTopicConfiguration,
      final String schemaFormat, final String schemaVersion, final String examples, final Boolean cloudEvent, final String keySelector) {
    streamBridgeMethods.add(MethodObject
                                .builder()
                                .operationId(operationId)
                                .channelName(channelName)
                                .classNamespace(classNamespace)
                                .type(STREAM_BRIDGE)
                                .action(action)
                                .keyClassNamespace(bindings)
                                .bindingType(bindingType)
                                .serverBindings(serverBindings)
                                .channelBindings(channelBindings)
                                .operationBindings(operationBindings)
                                .messageBindings(messageBindings)
                                .securityRequirements(securityRequirements)
                                .securitySchemes(securitySchemes)
                                .channelParameters(channelParameters)
                                .bindingVersion(bindingVersion)
                                .mqttQos(mqttQos)
                                .mqttRetain(mqttRetain)
                                .websocketMethod(websocketMethod)
                                .websocketSubprotocol(websocketSubprotocol)
                                .websocketHeaders(websocketHeaders)
                                .kafkaSaslMechanism(kafkaSaslMechanism)
                                .kafkaSecurityProtocol(kafkaSecurityProtocol)
                                .kafkaPartition(kafkaPartition)
                                .kafkaHeaders(kafkaHeaders)
                                .kafkaTopicConfiguration(kafkaTopicConfiguration)
                                .schemaFormat(schemaFormat)
                                .schemaVersion(schemaVersion)
                                .correlationId(correlationId)
                                .causationId(causationId)
                                .replyTo(replyTo)
                                .examples(examples)
                                .cloudEvent(cloudEvent)
                                .keySelector(keySelector)
                                .build());
  }

  public final void addSubscribeMethod(
      final String operationId, final String classNamespace, final String channelName, final String bindings, final String bindingType, final String action,
      final String serverBindings,
      final String channelBindings, final String operationBindings, final String messageBindings, final String securityRequirements, final String securitySchemes,
      final String channelParameters, final String correlationId, final String causationId, final String replyTo, final String bindingVersion, final Integer mqttQos,
      final Boolean mqttRetain, final String websocketMethod, final String websocketSubprotocol, final String websocketHeaders, final String kafkaSaslMechanism,
      final String kafkaSecurityProtocol, final Integer kafkaPartition, final String kafkaHeaders, final String kafkaTopicConfiguration, final String schemaFormat,
      final String schemaVersion, final String examples, final Boolean cloudEvent, final String keySelector) {
    subscribeMethods.add(MethodObject
                             .builder()
                             .operationId(operationId)
                             .classNamespace(classNamespace)
                             .channelName(channelName)
                             .type(SUBSCRIBE)
                             .action(action)
                             .keyClassNamespace(bindings)
                             .bindingType(bindingType)
                             .serverBindings(serverBindings)
                             .channelBindings(channelBindings)
                             .operationBindings(operationBindings)
                             .messageBindings(messageBindings)
                             .securityRequirements(securityRequirements)
                             .securitySchemes(securitySchemes)
                             .channelParameters(channelParameters)
                             .bindingVersion(bindingVersion)
                             .mqttQos(mqttQos)
                             .mqttRetain(mqttRetain)
                             .websocketMethod(websocketMethod)
                             .websocketSubprotocol(websocketSubprotocol)
                             .websocketHeaders(websocketHeaders)
                             .kafkaSaslMechanism(kafkaSaslMechanism)
                             .kafkaSecurityProtocol(kafkaSecurityProtocol)
                             .kafkaPartition(kafkaPartition)
                             .kafkaHeaders(kafkaHeaders)
                             .kafkaTopicConfiguration(kafkaTopicConfiguration)
                             .schemaFormat(schemaFormat)
                             .schemaVersion(schemaVersion)
                             .correlationId(correlationId)
                             .causationId(causationId)
                             .replyTo(replyTo)
                             .examples(examples)
                             .cloudEvent(cloudEvent)
                             .keySelector(keySelector)
                             .build());
  }

  public final void setSupplierEntitiesSuffix(final String suffix) {
    addToRoot(SUPPLIER_ENTITIES_SUFFIX, suffix);
  }

  public final void setStreamBridgeEntitiesSuffix(final String suffix) {
    addToRoot(STREAM_BRIDGE_ENTITIES_SUFFIX, suffix);
  }

  public final void setSubscribeEntitiesSuffix(final String suffix) {
    addToRoot(SUBSCRIBE_ENTITIES_SUFFIX, suffix);
  }

  public final void calculateJavaEEPackage(final Integer springBootVersion) {
    if (3 <= springBootVersion) {
      addToRoot(JAVA_EE_PACKAGE, JAKARTA);
      addToRoot(KAFKA_KEY_HEADER, KEY);
    } else {
      addToRoot(JAVA_EE_PACKAGE, JAVAX);
      addToRoot(KAFKA_KEY_HEADER, MESSAGE_KEY);
    }
  }

  public final void clearData() {
    cleanData();
    publishMethods.clear();
    subscribeMethods.clear();
    streamBridgeMethods.clear();
  }

  @Override
  protected void clearRoot() {
    delFromRoot(CLASS_NAMESPACE);
    delFromRoot(CLASS_NAME);
    delFromRoot(KEY_NAMESPACE);
    delFromRoot(PUBLISH_METHODS);
    delFromRoot(SUBSCRIBE_METHODS);
    delFromRoot(STREAM_BRIDGE_METHODS);
  }

  public final void fillTemplateWrapper(
      final String modelPackage,
      final String classFullName,
      final String className,
      final String keyClassFullName,
      final String keyClassName
                                       ) throws IOException {
    final var filePath = processPath(getPath(modelPackage));
    addToRoot(Map.of(WRAPPER_PACKAGE, modelPackage,
                     CLASS_NAMESPACE, classFullName,
                     CLASS_NAME, className,
                     KEY_NAMESPACE, keyClassFullName,
                     KEY_CLASS_NAME, keyClassName));

    String wrapperName = NameUtils.withOneSuffix(className, WRAPPER_SUFFIX);
    writeTemplateToFile(
        TemplateIndexConstants.TEMPLATE_MESSAGE_WRAPPER,
        filePath,
        wrapperName);
  }

  public void processFilePaths(final SpecFile fileParameter, final String defaultApiPackage) {
    var pathToCreate = convertPackageToTargetPath(fileParameter.getSupplier(), defaultApiPackage);
    if (Objects.nonNull(pathToCreate)) {
      setSupplierFilePath(processPath(pathToCreate));
    }
    pathToCreate = convertPackageToTargetPath(fileParameter.getStreamBridge(), defaultApiPackage);
    if (Objects.nonNull(pathToCreate)) {
      setStreamBridgeFilePath(processPath(pathToCreate));
    }
    pathToCreate = convertPackageToTargetPath(fileParameter.getConsumer(), defaultApiPackage);
    if (Objects.nonNull(pathToCreate)) {
      setSubscribeFilePath(processPath(pathToCreate));
    }
  }

  public final void setSupplierFilePath(final Path path) {
    this.supplierFilePath = path.toString();
    if (this.schemaRegistryFilePath == null) {
      this.schemaRegistryFilePath = path.toString();
    }
  }

  public final void setStreamBridgeFilePath(final Path path) {
    this.streamBridgeFilePath = path.toString();
    this.schemaRegistryFilePath = path.toString();
  }

  public final void setSubscribeFilePath(final Path path) {
    this.subscribeFilePath = path.toString();
  }
}
