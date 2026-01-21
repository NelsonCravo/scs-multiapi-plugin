/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.sngular.api.generator.plugin.asyncapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessMethodResult {

  private String operationId;

  private String namespace;

  private JsonNode payload;

  private String bindings;

  private String bindingType;

  private String action;

  private String channelName;

  private String serverBindings;

  private String channelBindings;

  private String operationBindings;

  private String messageBindings;

  private String securityRequirements;

  private String securitySchemes;

  private String channelParameters;

  private String bindingVersion;

  private Integer mqttQos;

  private Boolean mqttRetain;

  private String websocketMethod;

  private String websocketSubprotocol;

  private String websocketHeaders;

  private String kafkaSaslMechanism;

  private String kafkaSecurityProtocol;

  private Integer kafkaPartition;

  private String kafkaHeaders;

  private String kafkaTopicConfiguration;

  private String schemaFormat;

  private String schemaVersion;

  private String correlationId;

  private String causationId;

  private String replyTo;

  private String examples;

  private Boolean cloudEvent;

  private String keySelector;

}
