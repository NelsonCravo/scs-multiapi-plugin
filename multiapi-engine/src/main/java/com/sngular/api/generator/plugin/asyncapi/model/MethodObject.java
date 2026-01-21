/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.sngular.api.generator.plugin.asyncapi.model;

import java.util.Objects;

import com.sngular.api.generator.plugin.asyncapi.util.BindingTypeEnum;
import com.sngular.api.generator.plugin.common.model.SchemaObject;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
public class MethodObject {

  String operationId;

  String classNamespace;

  String className;

  String type;

  String action;

  String channelName;

  SchemaObject schemaObject;

  String keyClassName;

  String keyClassNamespace;

  String bindingType;

  String serverBindings;

  String channelBindings;

  String operationBindings;

  String messageBindings;

  String securityRequirements;

  String securitySchemes;

  String channelParameters;

  String bindingVersion;

  Integer mqttQos;

  Boolean mqttRetain;

  String websocketMethod;

  String websocketSubprotocol;

  String websocketHeaders;

  String kafkaSaslMechanism;

  String kafkaSecurityProtocol;

  Integer kafkaPartition;

  String kafkaHeaders;

  String kafkaTopicConfiguration;

  String schemaFormat;

  String schemaVersion;

  String correlationId;

  String causationId;

  String replyTo;

  String examples;

  Boolean cloudEvent;

  String keySelector;

  @Builder(toBuilder = true)
  public MethodObject(
      final String operationId, final String classNamespace, final String type, final String channelName, final SchemaObject schemaObject,
      final String action,
      final String keyClassNamespace, final String bindingType, final String serverBindings, final String channelBindings, final String operationBindings,
      final String messageBindings, final String securityRequirements, final String securitySchemes, final String channelParameters, final String correlationId,
      final String causationId, final String replyTo, final String bindingVersion, final Integer mqttQos, final Boolean mqttRetain,
      final String websocketMethod, final String websocketSubprotocol, final String websocketHeaders, final String kafkaSaslMechanism, final String kafkaSecurityProtocol,
      final Integer kafkaPartition, final String kafkaHeaders, final String kafkaTopicConfiguration, final String schemaFormat, final String schemaVersion,
      final String examples, final Boolean cloudEvent, final String keySelector) {
    this.operationId = operationId;
    this.classNamespace = classNamespace.substring(0, classNamespace.lastIndexOf("."));
    this.className = classNamespace.substring(classNamespace.lastIndexOf(".") + 1);
    this.type = type;
    this.action = StringUtils.defaultIfBlank(action, type);
    this.channelName = channelName;
    this.schemaObject = schemaObject;
    if (Objects.nonNull(keyClassNamespace)) {
      if (keyClassNamespace.contains(".")) {
        this.keyClassNamespace = keyClassNamespace.substring(0, keyClassNamespace.lastIndexOf("."));
        this.keyClassName = keyClassNamespace.substring(keyClassNamespace.lastIndexOf(".") + 1);
      } else {
        this.keyClassNamespace = null;
        this.keyClassName = keyClassNamespace;
      }
    } else {
      this.keyClassName = null;
      this.keyClassNamespace = null;
    }
    this.bindingType = StringUtils.isEmpty(bindingType) ? BindingTypeEnum.NONBINDING.getValue() : bindingType;
    this.serverBindings = serverBindings;
    this.channelBindings = channelBindings;
    this.operationBindings = operationBindings;
    this.messageBindings = messageBindings;
    this.securityRequirements = securityRequirements;
    this.securitySchemes = securitySchemes;
    this.channelParameters = channelParameters;
    this.bindingVersion = bindingVersion;
    this.mqttQos = mqttQos;
    this.mqttRetain = mqttRetain;
    this.websocketMethod = websocketMethod;
    this.websocketSubprotocol = websocketSubprotocol;
    this.websocketHeaders = websocketHeaders;
    this.kafkaSaslMechanism = kafkaSaslMechanism;
    this.kafkaSecurityProtocol = kafkaSecurityProtocol;
    this.kafkaPartition = kafkaPartition;
    this.kafkaHeaders = kafkaHeaders;
    this.kafkaTopicConfiguration = kafkaTopicConfiguration;
    this.schemaFormat = schemaFormat;
    this.schemaVersion = schemaVersion;
    this.correlationId = correlationId;
    this.causationId = causationId;
    this.replyTo = replyTo;
    this.examples = examples;
    this.cloudEvent = cloudEvent;
    this.keySelector = keySelector;
  }

}
