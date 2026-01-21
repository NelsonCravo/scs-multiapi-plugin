/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.sngular.api.generator.plugin.asyncapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessBindingsResult {

  private String bindings;

  private String bindingType;

  private String serverBindings;

  private String channelBindings;

  private String operationBindings;

  private String messageBindings;

  private String securityRequirements;

  private String securitySchemes;

  private String bindingVersion;

  private Integer mqttQos;

  private Boolean mqttRetain;

  private String websocketMethod;

  private String websocketSubprotocol;

  private String websocketHeaders;

  private String examples;

  private String keySelector;

  private Integer kafkaPartition;

  private String kafkaHeaders;

  private String kafkaTopicConfiguration;

  public static ProcessBindingsResultBuilder builder() {
    return new ProcessBindingsResultBuilder();
  }

  public static class ProcessBindingsResultBuilder {

    private String bindings;
    private String bindingType;
    private String serverBindings;
    private String channelBindings;
    private String operationBindings;
    private String messageBindings;
    private String securityRequirements;
    private String securitySchemes;
    private String bindingVersion;
    private Integer mqttQos;
    private Boolean mqttRetain;
    private String websocketMethod;
    private String websocketSubprotocol;
    private String websocketHeaders;
    private String examples;
    private String keySelector;
    private Integer kafkaPartition;
    private String kafkaHeaders;
    private String kafkaTopicConfiguration;

    ProcessBindingsResultBuilder() {
    }

    public ProcessBindingsResultBuilder bindings(final String bindings) {
      this.bindings = bindings;
      return this;
    }

    public ProcessBindingsResultBuilder bindingType(final String bindingType) {
      this.bindingType = bindingType;
      return this;
    }

    public ProcessBindingsResultBuilder serverBindings(final String serverBindings) {
      this.serverBindings = serverBindings;
      return this;
    }

    public ProcessBindingsResultBuilder channelBindings(final String channelBindings) {
      this.channelBindings = channelBindings;
      return this;
    }

    public ProcessBindingsResultBuilder operationBindings(final String operationBindings) {
      this.operationBindings = operationBindings;
      return this;
    }

    public ProcessBindingsResultBuilder messageBindings(final String messageBindings) {
      this.messageBindings = messageBindings;
      return this;
    }

    public ProcessBindingsResultBuilder securityRequirements(final String securityRequirements) {
      this.securityRequirements = securityRequirements;
      return this;
    }

    public ProcessBindingsResultBuilder securitySchemes(final String securitySchemes) {
      this.securitySchemes = securitySchemes;
      return this;
    }

    public ProcessBindingsResultBuilder bindingVersion(final String bindingVersion) {
      this.bindingVersion = bindingVersion;
      return this;
    }

    public ProcessBindingsResultBuilder mqttQos(final Integer mqttQos) {
      this.mqttQos = mqttQos;
      return this;
    }

    public ProcessBindingsResultBuilder mqttRetain(final Boolean mqttRetain) {
      this.mqttRetain = mqttRetain;
      return this;
    }

    public ProcessBindingsResultBuilder websocketMethod(final String websocketMethod) {
      this.websocketMethod = websocketMethod;
      return this;
    }

    public ProcessBindingsResultBuilder websocketSubprotocol(final String websocketSubprotocol) {
      this.websocketSubprotocol = websocketSubprotocol;
      return this;
    }

    public ProcessBindingsResultBuilder websocketHeaders(final String websocketHeaders) {
      this.websocketHeaders = websocketHeaders;
      return this;
    }

    public ProcessBindingsResultBuilder examples(final String examples) {
      this.examples = examples;
      return this;
    }

    public ProcessBindingsResultBuilder keySelector(final String keySelector) {
      this.keySelector = keySelector;
      return this;
    }

    public ProcessBindingsResultBuilder kafkaPartition(final Integer kafkaPartition) {
      this.kafkaPartition = kafkaPartition;
      return this;
    }

    public ProcessBindingsResultBuilder kafkaHeaders(final String kafkaHeaders) {
      this.kafkaHeaders = kafkaHeaders;
      return this;
    }

    public ProcessBindingsResultBuilder kafkaTopicConfiguration(final String kafkaTopicConfiguration) {
      this.kafkaTopicConfiguration = kafkaTopicConfiguration;
      return this;
    }

    public ProcessBindingsResult build() {
      return new ProcessBindingsResult(bindings, bindingType, serverBindings, channelBindings, operationBindings, messageBindings, securityRequirements, securitySchemes,
          bindingVersion, mqttQos, mqttRetain, websocketMethod, websocketSubprotocol, websocketHeaders, examples, keySelector, kafkaPartition, kafkaHeaders,
          kafkaTopicConfiguration);
    }
  }

}
