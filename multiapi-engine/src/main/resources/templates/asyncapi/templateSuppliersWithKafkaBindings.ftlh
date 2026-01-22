package ${supplierPackage};

import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.MDC;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonPointer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
<#list publishMethods as method>
import ${method.classNamespace}.${method.className}<#if supplierEntitiesSuffix?has_content>${supplierEntitiesSuffix}</#if>;
<#if method.keyClassNamespace?has_content>
import ${method.keyClassNamespace}.${method.keyClassName};
</#if></#list>
import ${wrapperPackage}.MessageWrapper;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.kafka.KafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaConsumerFactoryCustomizer;

@Configuration
public class ${supplierClassName?cap_first} {

  <#list publishMethods as method>
  private final I${method.operationId?cap_first} ${method.operationId?uncap_first};
  <#if method.replyTo?has_content>
  private long ${method.operationId?uncap_first}ReplyTimeoutMs;
  private String ${method.operationId?uncap_first}ReplyTopicOverride;
  </#if>

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

  protected ${supplierClassName?cap_first}(<#list publishMethods as method>final I${method.operationId?cap_first} ${method.operationId?uncap_first}<#sep>, </#list>,
      final Environment environment, final ObjectMapper objectMapper) {
    <#list publishMethods as method>
    this.${method.operationId?uncap_first} = ${method.operationId?uncap_first};
    </#list>
    this.environment = environment;
    this.objectMapper = objectMapper;
    <#list publishMethods as method>
      <#if method.replyTo?has_content>
        this.${method.operationId?uncap_first}ReplyTimeoutMs = Long.parseLong(environment.getProperty("multiapi.reply.this.${method.operationId?lower_case}.timeout-ms", 5000));
        this.${method.operationId?uncap_first}ReplyTopicOverride = environment.getProperty("multiapi.reply.this.${method.operationId?lower_case}.topic", "");
      </#if>
    </#list>
  }

  <#if publishMethods?size gt 0>
  @PostConstruct
  public void applyKafkaSecurityDefaults() {
    <#list publishMethods as method>
    <#if method.kafkaSaslMechanism?has_content>
    System.setProperty("spring.kafka.properties.sasl.mechanism",
        System.getProperty("spring.kafka.properties.sasl.mechanism", "${method.kafkaSaslMechanism}"));
    </#if>
    <#if method.kafkaSecurityProtocol?has_content>
    System.setProperty("spring.kafka.properties.security.protocol",
        System.getProperty("spring.kafka.properties.security.protocol", "${method.kafkaSecurityProtocol}"));
    </#if>
    <#if method.mqttQos?has_content>
    System.setProperty("spring.kafka.producer.acks",
        System.getProperty("spring.kafka.producer.acks", "${method.mqttQos}"));
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
      System.setProperty("spring.kafka.properties.value.serializer",
          System.getProperty("spring.kafka.properties.value.serializer", resolveSchemaRegistrySerializer()));
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
    final Map<String, Object> security = kafkaSecurityConfigs();
    return factory -> {
      if (!security.isEmpty()) {
        factory.updateConfigs(security);
      }
    };
  }

  @Bean
  public KafkaConsumerFactoryCustomizer<Object, Object> multiapiKafkaConsumerSecurityCustomizer() {
    final Map<String, Object> security = kafkaSecurityConfigs();
    return factory -> {
      if (!security.isEmpty()) {
        factory.updateConfigs(security);
      }
    };
  }

  private Map<String, Object> kafkaSecurityConfigs() {
    final Map<String, Object> configs = new HashMap<>();
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

  private String resolveSchemaRegistrySerializer() {
    String serializer = null;
    <#list publishMethods as method>
    <#if !serializer?has_content && method.schemaFormat?has_content>
      <#assign formatLower = method.schemaFormat?lower_case>
      <#if formatLower?contains("avro")>
        <#assign serializer = "io.confluent.kafka.serializers.KafkaAvroSerializer">
      <#elseif formatLower?contains("proto")>
        <#assign serializer = "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer">
      <#else>
        <#assign serializer = "io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer">
      </#if>
    </#if>
    </#list>
    return serializer != null ? serializer : "io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer";
  }

<#list publishMethods as method>
  @Bean
  public Supplier<Message<${method.className}<#if supplierEntitiesSuffix?has_content>${supplierEntitiesSuffix}</#if>>> ${method.operationId?uncap_first}() {
    final var messageWrapper =  ${method.operationId?uncap_first}.${method.operationId?uncap_first}();
    final String resolvedCorrelationId = messageWrapper.getCorrelationId() != null ? messageWrapper.getCorrelationId() : UUID.randomUUID().toString();
    final String configuredReplyTopic = <#if method.replyTo?has_content>${method.operationId?uncap_first}ReplyTopicOverride</#if><#if !method.replyTo?has_content>null</#if>;
    final String resolvedReplyTo = configuredReplyTopic != null && !configuredReplyTopic.isBlank()
        ? configuredReplyTopic
        : (messageWrapper.getReplyTo() != null
            ? messageWrapper.getReplyTo()
            : <#if method.replyTo?has_content>"${method.replyTo}"<#else>null</#if>);
    final String mdcTraceId = MDC.get("traceId");
    final String mdcTraceparent = MDC.get("traceparent");
    final Object resolvedKey = <#if method.keySelector?has_content>evaluateKeySelector(messageWrapper.getPayload(), "${method.keySelector}")<#else>null</#if>;
    final Object finalKey = messageWrapper.getKey() != null ? messageWrapper.getKey() : resolvedKey;
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
    <#if method.correlationId?has_content || method.causationId?has_content || method.replyTo?has_content>
    // correlation: id=${method.correlationId!"-"}, causation=${method.causationId!"-"}, replyTo=${method.replyTo!"-"}
    </#if>
    <#if method.examples?has_content>
    // examples: ${method.examples}
    </#if>
    <#if method.cloudEvent?? && method.cloudEvent>
    // CloudEvent: headers will be propagated (specversion/id/type/source/subject/time/dataSchema/dataContentType)
    </#if>
    final var messageBuilder = MessageBuilder.withPayload((${method.className}<#if supplierEntitiesSuffix?has_content>${supplierEntitiesSuffix}</#if>) messageWrapper.getPayload())
                               .setHeader(KafkaHeaders.${kafkaKeyHeader}, (<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>) finalKey);
                               <#if method.kafkaSaslMechanism?has_content>
    messageBuilder.setHeader("kafka_sasl_mechanism", "${method.kafkaSaslMechanism}");
                               </#if>
                               <#if method.kafkaSecurityProtocol?has_content>
    messageBuilder.setHeader("kafka_security_protocol", "${method.kafkaSecurityProtocol}");
                               </#if>
                               <#if method.kafkaPartition?has_content>
    messageBuilder.setHeader(KafkaHeaders.PARTITION_ID, ${method.kafkaPartition});
                               </#if>
    messageBuilder.setHeader(KafkaHeaders.CORRELATION_ID, resolvedCorrelationId);
    if (resolvedReplyTo != null) {
      messageBuilder.setHeader(KafkaHeaders.REPLY_TOPIC, resolvedReplyTo);
    }
    if (multiapiKafkaApiKeyHeader != null && !multiapiKafkaApiKeyHeader.isBlank() && multiapiKafkaApiKeyValue != null && !multiapiKafkaApiKeyValue.isBlank()) {
      messageBuilder.setHeader(multiapiKafkaApiKeyHeader, multiapiKafkaApiKeyValue);
    }
    <#if method.cloudEvent?? && method.cloudEvent>
    final String ceId = messageWrapper.getCeId() != null ? messageWrapper.getCeId() : resolvedCorrelationId;
    final String ceType = messageWrapper.getCeType() != null ? messageWrapper.getCeType() : "${method.operationId}";
    final String ceSource = messageWrapper.getCeSource() != null ? messageWrapper.getCeSource() : "urn:channel:${method.channelName}";
    messageBuilder.setHeader("ce_specversion", messageWrapper.getCeSpecVersion() != null ? messageWrapper.getCeSpecVersion() : "1.0");
    messageBuilder.setHeader("ce_id", ceId);
    messageBuilder.setHeader("ce_type", ceType);
    messageBuilder.setHeader("ce_source", ceSource);
    if (messageWrapper.getCeSubject() != null) { messageBuilder.setHeader("ce_subject", messageWrapper.getCeSubject()); }
    if (messageWrapper.getCeTime() != null) { messageBuilder.setHeader("ce_time", messageWrapper.getCeTime()); }
    if (messageWrapper.getCeDataSchema() != null) { messageBuilder.setHeader("ce_dataschema", messageWrapper.getCeDataSchema()); }
    if (messageWrapper.getCeDataContentType() != null) { messageBuilder.setHeader("ce_datacontenttype", messageWrapper.getCeDataContentType()); }
    </#if>
    if (mdcTraceId != null) {
      messageBuilder.setHeader("traceid", mdcTraceId);
    }
    if (mdcTraceparent != null) {
      messageBuilder.setHeader("traceparent", mdcTraceparent);
    }
    return () -> messageBuilder.build();
  }

  <#if method.replyTo?has_content>
  @Bean
  public ContainerProperties ${method.operationId?uncap_first}ReplyContainerProperties() {
    final String targetReplyTopic = (${method.operationId?uncap_first}ReplyTopicOverride != null && !${method.operationId?uncap_first}ReplyTopicOverride.isBlank())
        ? ${method.operationId?uncap_first}ReplyTopicOverride
        : "${method.replyTo}";
    final var props = new ContainerProperties(targetReplyTopic);
    props.setGroupId("${method.operationId?uncap_first}-reply-group");
    return props;
  }

  @Bean
  public KafkaMessageListenerContainer<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}> ${method.operationId?uncap_first}ReplyContainer(
      final ConsumerFactory<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}> consumerFactory) {
    return new KafkaMessageListenerContainer<>(consumerFactory, ${method.operationId?uncap_first}ReplyContainerProperties());
  }

  @Bean
  public ReplyingKafkaTemplate<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}, ${method.className}> ${method.operationId?uncap_first}ReplyingKafkaTemplate(
      final ProducerFactory<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}> producerFactory,
      final KafkaMessageListenerContainer<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}> ${method.operationId?uncap_first}ReplyContainer) {
    final var template = new ReplyingKafkaTemplate<>(producerFactory, ${method.operationId?uncap_first}ReplyContainer);
    template.setDefaultReplyTimeout(Duration.ofMillis(${method.operationId?uncap_first}ReplyTimeoutMs));
    return template;
  }

  @Bean
  public ${method.operationId?cap_first}ReplyRegistry ${method.operationId?uncap_first}ReplyRegistry() {
    return new ${method.operationId?cap_first}ReplyRegistry();
  }

  @Bean
  public ContainerProperties ${method.operationId?uncap_first}ReplyFallbackContainerProperties() {
    final String targetReplyTopic = (${method.operationId?uncap_first}ReplyTopicOverride != null && !${method.operationId?uncap_first}ReplyTopicOverride.isBlank())
        ? ${method.operationId?uncap_first}ReplyTopicOverride
        : "${method.replyTo}";
    final var props = new ContainerProperties(targetReplyTopic);
    props.setGroupId("${method.operationId?uncap_first}-reply-fallback");
    return props;
  }

  @Bean
  public KafkaMessageListenerContainer<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}> ${method.operationId?uncap_first}ReplyFallbackContainer(
      final ConsumerFactory<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}> consumerFactory,
      final ${method.operationId?cap_first}ReplyRegistry registry) {
    final var container = new KafkaMessageListenerContainer<>(consumerFactory, ${method.operationId?uncap_first}ReplyFallbackContainerProperties());
    container.setupMessageListener((MessageListener<<#if method.keyClassName?has_content>${method.keyClassName?cap_first}<#else>Object</#if>, ${method.className}>)
        message -> {
          final String correlation = message.getHeaders().get(KafkaHeaders.CORRELATION_ID, String.class);
          registry.complete(correlation, message.getPayload());
        });
    return container;
  }
  </#if>

</#list>

<#list publishMethods as method>
<#if method.replyTo?has_content>
  public static class ${method.operationId?cap_first}ReplyRegistry {
    private final ConcurrentHashMap<String, CompletableFuture<${method.className}>> inflight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Consumer<${method.className}>> handlers = new ConcurrentHashMap<>();

    public CompletableFuture<${method.className}> register(final String correlationId) {
      final var future = new CompletableFuture<${method.className}>();
      inflight.put(correlationId, future);
      return future;
    }

    public void registerHandler(final String correlationId, final Consumer<${method.className}> handler) {
      if (handler != null) {
        handlers.put(correlationId, handler);
      }
    }

    public void complete(final String correlationId, final ${method.className} payload) {
      final var handler = handlers.remove(correlationId);
      if (handler != null) {
        handler.accept(payload);
        inflight.remove(correlationId);
        return;
      }
      final var future = inflight.remove(correlationId);
      if (future != null) {
        future.complete(payload);
      }
    }
  }
</#if>
</#list>

  private Object evaluateKeySelector(final Object payload, final String selector) {
    if (payload == null || selector == null || selector.isBlank()) {
      return null;
    }
    try {
      final JsonNode tree = objectMapper.valueToTree(payload);
      final String pointerExpr = selector.startsWith("$.") ? selector.substring(1).replace(".", "/") : selector;
      final JsonPointer pointer = JsonPointer.compile(pointerExpr);
      final JsonNode node = tree.at(pointer);
      if (node.isMissingNode() || node.isNull()) {
        return null;
      }
      if (node.isTextual()) {
        return node.asText();
      }
      if (node.isNumber()) {
        return node.numberValue();
      }
      if (node.isBoolean()) {
        return node.asBoolean();
      }
      return node.toString();
    } catch (final Exception ignored) {
      return null;
    }
  }
}
