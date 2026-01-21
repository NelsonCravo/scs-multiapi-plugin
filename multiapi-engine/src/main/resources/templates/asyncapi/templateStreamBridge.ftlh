package ${streamBridgePackage};

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.core.env.Environment;
<#list streamBridgeMethods as method>
import ${method.classNamespace}.${method.className}<#if streamBridgeEntitiesSuffix?has_content>${streamBridgeEntitiesSuffix}</#if>;
</#list>

@Component
public class ${streamBridgeClassName?cap_first} {

  private final StreamBridge streamBridge;
  private final Environment environment;
  private static final Pattern CHANNEL_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

  public ${streamBridgeClassName?cap_first}(final StreamBridge streamBridge, final Environment environment) {
    this.streamBridge = streamBridge;
    this.environment = environment;
  }

<#list streamBridgeMethods as method>
  public void ${method.operationId?uncap_first}(final ${method.className}<#if streamBridgeEntitiesSuffix?has_content>${streamBridgeEntitiesSuffix}</#if> ${method.className?uncap_first}) {
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
    final String resolvedChannel = resolveChannel("${method.channelName}");
    streamBridge.send(resolvedChannel, ${method.className?uncap_first});
  }

  </#list>

  private String resolveChannel(final String channelTemplate) {
    final Matcher matcher = CHANNEL_PARAM_PATTERN.matcher(channelTemplate);
    final StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      final String param = matcher.group(1);
      final String value = environment.getProperty("multiapi.channel." + param,
          environment.getProperty(param, System.getenv(param)));
      matcher.appendReplacement(buffer, value != null ? Matcher.quoteReplacement(value) : param);
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }
}
