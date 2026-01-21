package ${supplierPackage};

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
<#list publishMethods as method>
import ${method.classNamespace}.${method.className}<#if supplierEntitiesSuffix?has_content>${supplierEntitiesSuffix}</#if>;
</#list>

@Configuration
public class ${supplierClassName?cap_first} {

  <#list publishMethods as method>
  private final I${method.operationId?cap_first} ${method.operationId?uncap_first};

  </#list>
  protected ${supplierClassName?cap_first}(<#list publishMethods as method>final I${method.operationId?cap_first} ${method.operationId?uncap_first}<#sep>, </#list>) {
    <#list publishMethods as method>
    this.${method.operationId?uncap_first} = ${method.operationId?uncap_first};
    </#list>
  }

<#list publishMethods as method>
  @Bean
  public Supplier<${method.className}<#if supplierEntitiesSuffix?has_content>${supplierEntitiesSuffix}</#if>> ${method.operationId?uncap_first}() {
    // action: ${method.action}
    <#if method.serverBindings?has_content || method.channelBindings?has_content || method.operationBindings?has_content || method.messageBindings?has_content>
    // bindings: server=${method.serverBindings!"-"}, channel=${method.channelBindings!"-"}, operation=${method.operationBindings!"-"}, message=${method.messageBindings!"-"}
    </#if>
    <#if method.securityRequirements?has_content || method.securitySchemes?has_content>
    // security: requirements=${method.securityRequirements!"-"}, schemes=${method.securitySchemes!"-"}
    </#if>
    <#if method.channelParameters?has_content>
    // channel parameters: ${method.channelParameters}
    </#if>
    <#if method.bindingVersion?has_content>
    // bindingVersion: ${method.bindingVersion}
    </#if>
    <#if method.mqttQos?has_content || method.mqttRetain?has_content>
    // mqtt: qos=${method.mqttQos!"-"}, retain=${method.mqttRetain?string("true","false","-")}
    </#if>
    <#if method.websocketMethod?has_content || method.websocketSubprotocol?has_content || method.websocketHeaders?has_content>
    // websocket: method=${method.websocketMethod!"-"}, subprotocol=${method.websocketSubprotocol!"-"}, headers=${method.websocketHeaders!"-"}
    </#if>
    <#if method.kafkaSaslMechanism?has_content>
    // kafka security: sasl=${method.kafkaSaslMechanism!"-"}, protocol=${method.kafkaSecurityProtocol!"-"}
    </#if>
    <#if method.schemaFormat?has_content || method.schemaVersion?has_content>
    // schema: format=${method.schemaFormat!"-"}, version=${method.schemaVersion!"-"}
    </#if>
    <#if method.correlationId?has_content || method.causationId?has_content || method.replyTo?has_content>
    // correlation: id=${method.correlationId!"-"}, causation=${method.causationId!"-"}, replyTo=${method.replyTo!"-"}
    </#if>
    return () -> ${method.operationId?uncap_first}.${method.operationId?uncap_first}();
  }

</#list>
}
