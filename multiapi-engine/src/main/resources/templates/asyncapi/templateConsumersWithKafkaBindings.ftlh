package ${subscribePackage};

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonPointer;
import org.springframework.boot.autoconfigure.kafka.KafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProducerFactoryCustomizer;
<#list subscribeMethods as method>
import ${method.classNamespace}.${method.className}<#if subscribeEntitiesSuffix?has_content>${subscribeEntitiesSuffix}</#if>;
<#if method.keyClassNamespace?has_content>
import ${method.keyClassNamespace}.${method.keyClassName};
</#if>
</#list>
import ${subscribePackage}.MessageWrapper;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

@Configuration
public class ${subscribeClassName?cap_first} {

  <#list subscribeMethods as method>
  private final I${method.operationId?cap_first} ${method.operationId?uncap_first};
  </#list>
  @Value("<#noparse>${multiapi.kafka.sasl.jaas-config:}</#noparse>")
  private String multiapiKafkaJaasConfig;
  @Value("<#noparse>${multiapi.kafka.ssl.truststore-location:}</#noparse>")
  private String multiapiKafkaTruststoreLocation;
  @Value("<#noparse>${multiapi.kafka.ssl.truststore-password:}</#noparse>")
  private String multiapiKafkaTruststorePassword;
  @Value("<#noparse>${multiapi.kafka.ssl.keystore-location:}</#noparse>")
  private String multiapiKafkaKeystoreLocation;
  @Value("<#noparse>${multiapi.kafka.ssl.keystore-password:}</#noparse>")
  private String multiapiKafkaKeystorePassword;
  @Value("<#noparse>${multiapi.kafka.api-key.header:}</#noparse>")
  private String multiapiKafkaApiKeyHeader;
  @Value("<#noparse>${multiapi.kafka.api-key.value:}</#noparse>")
  private String multiapiKafkaApiKeyValue;
  @Value("<#noparse>${multiapi.schema.registry.url:}</#noparse>")
  private String multiapiSchemaRegistryUrl;
  @Value("<#noparse>${multiapi.schema.registry.auto-register:true}</#noparse>")
  private String multiapiSchemaAutoRegister;
  private final Environment environment;
  private final ObjectMapper objectMapper;

  protected ${subscribeClassName?cap_first}(<#list subscribeMethods as method>final I${method.operationId?cap_first} ${method.operationId?uncap_first}<#sep>, </#list>,
      final Environment environment, final ObjectMapper objectMapper) {
    <#list subscribeMethods as method>
    this.${method.operationId?uncap_first} = ${method.operationId?uncap_first};
    </#list>
    this.environment = environment;
    this.objectMapper = objectMapper;
  }

<#list subscribeMethods as method>
  @Bean
  public Consumer<Message<${method.className}<#if subscribeEntitiesSuffix?has_content>${subscribeEntitiesSuffix}</#if>>> ${method.operationId?uncap_first}() {
    return value -> {
      final var previousTraceId = MDC.get("traceId");
      final var previousTraceparent = MDC.get("traceparent");
      final String ceIdHeader = <#if method.cloudEvent?? && method.cloudEvent>value.getHeaders().get("ce_id", String.class)</#if><#if !(method.cloudEvent?? && method.cloudEvent)>null</#if>;
      final String correlationHeader = value.getHeaders().get(KafkaHeaders.CORRELATION_ID, String.class);
      final String effectiveCorrelation = correlationHeader != null ? correlationHeader : ceIdHeader;
      if (value.getHeaders().get("traceid") != null) {
        MDC.put("traceId", value.getHeaders().get("traceid").toString());
      }
      if (value.getHeaders().get("traceparent") != null) {
        MDC.put("traceparent", value.getHeaders().get("traceparent").toString());
      }
      final var messageWrapper = MessageWrapper.<${method.className}<#if subscribeEntitiesSuffix?has_content>${subscribeEntitiesSuffix}</#if>, ${method.keyClassName?cap_first}>builder()
                                              .payload(value.getPayload())
                                              .key(resolveKey(value, ${method.keyClassName?cap_first}.class<#if method.keySelector?has_content>, "${method.keySelector}"</#if>))
                                              .correlationId(effectiveCorrelation)
                                              .replyTo(value.getHeaders().get(KafkaHeaders.REPLY_TOPIC, String.class))
                                              <#if method.cloudEvent?? && method.cloudEvent>
                                              .ceSpecVersion(value.getHeaders().get("ce_specversion", String.class))
                                              .ceId(value.getHeaders().get("ce_id", String.class))
                                              .ceType(value.getHeaders().get("ce_type", String.class))
                                              .ceSource(value.getHeaders().get("ce_source", String.class))
                                              .ceSubject(value.getHeaders().get("ce_subject", String.class))
                                              .ceTime(value.getHeaders().get("ce_time", String.class))
                                              .ceDataSchema(value.getHeaders().get("ce_dataschema", String.class))
                                              .ceDataContentType(value.getHeaders().get("ce_datacontenttype", String.class))
                                              </#if>
                                              .build();
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
      <#if method.kafkaPartition?has_content || method.kafkaTopicConfiguration?has_content || method.kafkaHeaders?has_content>
      // kafka extras: partition=${method.kafkaPartition!"-"}, topicConfig=${method.kafkaTopicConfiguration!"-"}, headers=${method.kafkaHeaders!"-"}
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
      <#if multiapiKafkaApiKeyHeader?has_content && multiapiKafkaApiKeyValue?has_content>
      // api-key header expected: ${multiapiKafkaApiKeyHeader}
      </#if>
      <#if method.correlationId?has_content || method.causationId?has_content || method.replyTo?has_content>
      // correlation: id=${method.correlationId!"-"}, causation=${method.causationId!"-"}, replyTo=${method.replyTo!"-"}
      </#if>
      <#if method.examples?has_content>
      // examples: ${method.examples}
      </#if>
      <#if method.cloudEvent?? && method.cloudEvent>
      // CloudEvent detected: attributes available in wrapper (ce_specversion/id/type/source/subject/time/dataSchema/dataContentType)
      </#if>
      <#if method.examples?has_content>
      // examples: ${method.examples}
      </#if>
      try {
        ${method.operationId?uncap_first}.${method.operationId?uncap_first}(messageWrapper);
      } finally {
        if (previousTraceId != null) {
          MDC.put("traceId", previousTraceId);
        } else {
          MDC.remove("traceId");
        }
        if (previousTraceparent != null) {
          MDC.put("traceparent", previousTraceparent);
        } else {
          MDC.remove("traceparent");
        }
      }
    };
  }

  </#list>

  <#if subscribeMethods?size gt 0>
  @PostConstruct
  public void applyKafkaSecurityDefaults() {
    <#list subscribeMethods as method>
    <#if method.kafkaSaslMechanism?has_content>
    System.setProperty("spring.kafka.properties.sasl.mechanism",
        System.getProperty("spring.kafka.properties.sasl.mechanism", "${method.kafkaSaslMechanism}"));
    </#if>
    <#if method.kafkaSecurityProtocol?has_content>
    System.setProperty("spring.kafka.properties.security.protocol",
        System.getProperty("spring.kafka.properties.security.protocol", "${method.kafkaSecurityProtocol}"));
    </#if>
    </#list>
    if (multiapiKafkaJaasConfig != null && !multiapiKafkaJaasConfig.isBlank()) {
      System.setProperty("spring.kafka.properties.sasl.jaas.config",
          System.getProperty("spring.kafka.properties.sasl.jaas.config", multiapiKafkaJaasConfig));
    }
    if (multiapiKafkaTruststoreLocation != null && !multiapiKafkaTruststoreLocation.isBlank()) {
      System.setProperty("spring.kafka.properties.ssl.truststore.location",
          System.getProperty("spring.kafka.properties.ssl.truststore.location", multiapiKafkaTruststoreLocation));
    }
    if (multiapiKafkaTruststorePassword != null && !multiapiKafkaTruststorePassword.isBlank()) {
      System.setProperty("spring.kafka.properties.ssl.truststore.password",
          System.getProperty("spring.kafka.properties.ssl.truststore.password", multiapiKafkaTruststorePassword));
    }
    if (multiapiKafkaKeystoreLocation != null && !multiapiKafkaKeystoreLocation.isBlank()) {
      System.setProperty("spring.kafka.properties.ssl.keystore.location",
          System.getProperty("spring.kafka.properties.ssl.keystore.location", multiapiKafkaKeystoreLocation));
    }
    if (multiapiKafkaKeystorePassword != null && !multiapiKafkaKeystorePassword.isBlank()) {
      System.setProperty("spring.kafka.properties.ssl.keystore.password",
          System.getProperty("spring.kafka.properties.ssl.keystore.password", multiapiKafkaKeystorePassword));
    }
    if (multiapiSchemaRegistryUrl != null && !multiapiSchemaRegistryUrl.isBlank()) {
      System.setProperty("spring.kafka.properties.schema.registry.url",
          System.getProperty("spring.kafka.properties.schema.registry.url", multiapiSchemaRegistryUrl));
      System.setProperty("spring.kafka.properties.value.deserializer",
          System.getProperty("spring.kafka.properties.value.deserializer", resolveSchemaRegistryDeserializer()));
      System.setProperty("spring.kafka.properties.auto.register.schemas",
          System.getProperty("spring.kafka.properties.auto.register.schemas", multiapiSchemaAutoRegister));
      final String creds = environment.getProperty("multiapi.schema.registry.basic-auth");
      if (creds != null && !creds.isBlank()) {
        System.setProperty("spring.kafka.properties.basic.auth.credentials.source",
            System.getProperty("spring.kafka.properties.basic.auth.credentials.source", "USER_INFO"));
        System.setProperty("spring.kafka.properties.basic.auth.user.info",
            System.getProperty("spring.kafka.properties.basic.auth.user.info", creds));
      }
    }
  }
  </#if>

  @Bean
  public KafkaProducerFactoryCustomizer<Object, Object> multiapiKafkaProducerSecurityCustomizer() {
    final java.util.Map<String, Object> security = kafkaSecurityConfigs();
    return factory -> {
      if (!security.isEmpty()) {
        factory.updateConfigs(security);
      }
    };
  }

  @Bean
  public KafkaConsumerFactoryCustomizer<Object, Object> multiapiKafkaConsumerSecurityCustomizer() {
    final java.util.Map<String, Object> security = kafkaSecurityConfigs();
    return factory -> {
      if (!security.isEmpty()) {
        factory.updateConfigs(security);
      }
    };
  }

  private java.util.Map<String, Object> kafkaSecurityConfigs() {
    final java.util.Map<String, Object> configs = new java.util.HashMap<>();
    final String saslMechanism = environment.getProperty("spring.kafka.properties.sasl.mechanism");
    final String securityProtocol = environment.getProperty("spring.kafka.properties.security.protocol");
    final String jaas = multiapiKafkaJaasConfig != null && !multiapiKafkaJaasConfig.isBlank()
        ? multiapiKafkaJaasConfig
        : environment.getProperty("spring.kafka.properties.sasl.jaas.config");
    final String truststore = multiapiKafkaTruststoreLocation;
    final String truststorePass = multiapiKafkaTruststorePassword;
    final String keystore = multiapiKafkaKeystoreLocation;
    final String keystorePass = multiapiKafkaKeystorePassword;
    if (saslMechanism != null && !saslMechanism.isBlank()) {
      configs.put("sasl.mechanism", saslMechanism);
    }
    if (securityProtocol != null && !securityProtocol.isBlank()) {
      configs.put("security.protocol", securityProtocol);
    }
    if (jaas != null && !jaas.isBlank()) {
      configs.put("sasl.jaas.config", jaas);
    }
    if (truststore != null && !truststore.isBlank()) {
      configs.put("ssl.truststore.location", truststore);
    }
    if (truststorePass != null && !truststorePass.isBlank()) {
      configs.put("ssl.truststore.password", truststorePass);
    }
    if (keystore != null && !keystore.isBlank()) {
      configs.put("ssl.keystore.location", keystore);
    }
    if (keystorePass != null && !keystorePass.isBlank()) {
      configs.put("ssl.keystore.password", keystorePass);
    }
    return configs;
  }

  private String resolveSchemaRegistryDeserializer() {
    String deserializer = null;
    <#list subscribeMethods as method>
    <#if !deserializer?has_content && method.schemaFormat?has_content>
      <#assign formatLower = method.schemaFormat?lower_case>
      <#if formatLower?contains("avro")>
        <#assign deserializer = "io.confluent.kafka.serializers.KafkaAvroDeserializer">
      <#elseif formatLower?contains("proto")>
        <#assign deserializer = "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer">
      <#else>
        <#assign deserializer = "io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer">
      </#if>
    </#if>
    </#list>
    return deserializer != null ? deserializer : "io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer";
  }

  <#-- Compute flag whether any subscribe method has a keySelector -->
  <#assign hasKeySelector=false />
  <#list subscribeMethods as _m>
    <#if _m.keySelector?has_content>
      <#assign hasKeySelector=true />
    </#if>
  </#list>

  private <K> K resolveKey(final Message<?> message, final Class<K> targetType<#if hasKeySelector>, final String selector</#if>) {
    final K headerKey = message.getHeaders().get(KafkaHeaders.${kafkaKeyHeader}, targetType);
    if (headerKey != null) {
      return headerKey;
    }
    <#if hasKeySelector>
    if (selector != null && !selector.isBlank()) {
      try {
        final JsonNode tree = objectMapper.valueToTree(message.getPayload());
        final String pointerExpr = selector.startsWith("$.") ? selector.substring(1).replace(".", "/") : selector;
        final JsonPointer pointer = JsonPointer.compile(pointerExpr);
        final JsonNode node = tree.at(pointer);
        if (node.isMissingNode() || node.isNull()) {
          return null;
        }
        if (targetType.equals(String.class) && node.isTextual()) {
          return targetType.cast(node.asText());
        }
        if (Number.class.isAssignableFrom(targetType) && node.isNumber()) {
          return targetType.cast(node.numberValue());
        }
        if (targetType.equals(Boolean.class) && node.isBoolean()) {
          return targetType.cast(node.asBoolean());
        }
        if (targetType.equals(Object.class)) {
          if (node.isTextual()) {
            return targetType.cast(node.asText());
          }
          if (node.isNumber()) {
            return targetType.cast(node.numberValue());
          }
          if (node.isBoolean()) {
            return targetType.cast(node.asBoolean());
          }
          return targetType.cast(node.toString());
        }
      } catch (final Exception ignored) {
        return null;
      }
    }
    </#if>
    return null;
  }
}
