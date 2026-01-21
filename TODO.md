## Pending items

ok - Fix OpenApiGenerator call in Gradle task: swapped arguments cause MissingMethodException when running openApiTask (OpenApiTask.groovy (line 32)).
ok - Adjust OpenAPI model generation to honor the computed package and avoid incorrect fallback (OpenApiGenerator.java:160-166,253-263).
✓ - Reset client/auth generation state per spec so `callMode` does not leak across files (`multiapi-engine/src/main/java/com/sngular/api/generator/plugin/openapi/OpenApiGenerator.java:59-66,81-134,168-179`).
✓ - Review Spring Boot 2 vs 3: ensure `jakarta` imports (no leftover `javax`) in templates/validators, use `KafkaHeaders.KEY` on Spring 3, and consider generating synchronous clients with `RestClient` when `springBootVersion >= 3`.
ok - Missing support for alias/nickname different from the class name (feature present in OpenAPI) to choose generated names.
ok - AsyncAPI: handle `allOf/oneOf/anyOf` generating a type/interface to generalize objects, similar to OpenAPI support.

ok - `multipleOf` where the generator failed to add import java.math.BigDecimal when present in YAML.
ok - If you need an alias different from the class name, we cannot include the nickname as happens in OpenAPI.
ok - If the title or message name is camelCase, treat as PascalCase.
ok - If the title or message name has hyphen, underscore, or special chars, normalize to Java naming.
ok - If Spring version is 3, handle as such: e.g., do not use KafkaHeaders.MESSAGE_KEY but KafkaHeaders.KEY, and use jakarta.validation instead of java.validation, plus other Spring 2 vs 3 differences.
ok - Full document metadata (title, version, description, contact, license, tags, externalDocs, x-extensions, governance) not yet propagated to generated code; map and expose (OpenAPI/AsyncAPI) in JavaDoc/annotations/constants.
ok - Description of multiple servers/protocols (Kafka, MQTT, AMQP, WebSockets, STOMP, HTTP, etc.) with per-server security properties not yet generated; map and expose in code/clients.
ok - Operations with action send/receive: replace publish/subscribe with a clear definition of what the service does with the channel.
ok - Schemas and validation: support complex schemas (JSON Schema/Avro), schema reuse, advanced validations.
ok - Advanced protocol bindings: allow configuring server/channel/operation/message bindings (partitions, QoS, encoding, protocol-specific flags).
ok - Detailed security: reusable SecuritySchemes in components, applicable globally to servers or locally per operation.
ok - Correlation and retriggering: correlation headers (correlationId/causationId/replyTo) and request/reply flows.
ok - Dynamic channel parameters: support channel parameters (MQTT wildcards, path params).
ok - AsyncAPI 3.0: decoupled operations model (send/receive) with full `operations` parser and per-action generation.
- Rich protocol bindings (Kafka/MQTT/AMQP/WebSockets): finish runtime mapping (AMQP/HTTP/WebSockets), bindingVersion, and QoS/flags.
ok   - Kafka: map bindingVersion/acks/partitions/custom headers and apply to producer/consumer factories and Spring properties.
  - MQTT/AMQP/WebSockets: support qos/retain/routingKey/headers/subprotocols; generate protocol-specific properties and validate bindingVersion.
  - HTTP/WebSockets: reflect method/headers/subprotocol and map to generated clients/handlers.
- Security applied to generated code (servers/operations): generate real wiring (Spring Security/SASL/API Key/OAuth2) per server/operation:
ok - Expose in templates the applied securityScheme and generate minimal config (e.g., SASL/PLAIN with JAAS config, SSL truststore/keystore, API Key headers).
 - Allow override via external properties (spring.kafka.* / spring.cloud.stream.*) and by bindings/security in the contract.
ok - Generate helper beans (Kafka config/customizer) to apply server-specific credentials and secure operations (partial: set defaults for sasl.mechanism, security.protocol, sasl.jaas.config via bindings/env).
ok - Document how-to and generated config keys.
ok - Cover additional Kafka binding fields (partitions, headers/config, topic-level options) and reflect in factories/Spring properties.
ok - Traits: resolve traits via `$ref` and deeply merge headers/bindings/descriptions/correlation in operations/messages.
- schemaFormat/schemaVersion: missing concrete generation per format (native Avro/Protobuf) and version selection:
ok -   Detect `schemaFormat` and select pipeline (JSON Schema, embedded Avro, embedded Protobuf) — currently only unknown formats were logged.
  - Support embedded Avro (currently only external `avsc`): parse, generate classes, and integrate with registry (optional).
  - Support Protobuf (`schemaFormat` protobuf): parse and generate Java classes.
ok -   Versioning: use `schemaVersion` for name/package (suffix or namespace) to avoid conflicts between versions.
ok -   Adjust templates/filenames to include version when present.
ok - MessageTraits and OperationTraits: parse and auto-apply traits (headers, bindings, description, correlation) in generated classes/methods.
ok - Automatic Request/Reply (Kafka): full request/reply flow without manual tweaks:
ok    - Producer generates/propagates `correlationId` and `replyTo` (default via channelName-reply/x-reply-topic) and awaits response with ReplyingKafkaTemplate/listener with timeout/retries.
ok      - Generate ReplyingKafkaTemplate per operation (producerFactory + reply container) with configurable timeout.
ok      - Allow fallback to dedicated listener for scenarios without ReplyingKafkaTemplate.
ok        - Generate dedicated listener container consuming the reply topic and manually correlating (correlationId -> CompletableFuture/handler).
ok        - Expose hook to register callback/handler per operation when not using ReplyingKafkaTemplate.
ok - Expose properties to configure reply topics and timeouts via bindings/env.
ok - Consumer reads `replyTo`/`correlationId` and publishes the response to the indicated topic preserving correlation.
ok - Configure reply topic (per operation or default) and map via bindings/broker properties (e.g., `x-reply-topic` in contract or `${channelName}-reply` convention; override via environment property).
- Schemas with additional formats and versioning: support schemaFormat (Avro/Protobuf/JSON Schema) and multiple versions of the same schema.
ok - JSON Schema: validate refs/imports with versioned namespace; apply `schemaVersion` to names/packages and adjust internal refs.
ok - JSON Schema: select serializer/deserializer and Schema Registry per `schemaFormat`; version namespaces (`_v<schemaVersion>`).
 - Avro: parse/generate classes (embedded), support multiple versions, select serializer/registry per version.
 - Protobuf: parse/generate classes (embedded), support multiple versions, select serializer/registry per version.
- Generated documentation: produce readable HTML/Markdown from the AsyncAPI contract.
  - Generate docs with servers/bindings/security/traits/operations send/receive, including payload/header examples (correlationId/replyTo).
  - Include table of bindings per protocol and suggested runtime properties.
 - Advanced correlation IDs: automatically extract standard properties (traceid/traceparent), propagate in messages, integrate with Sleuth/Micrometer, and register in MDC.
 - Message examples: use `examples` from bindings/payloads for Javadoc, test builders, and generated sample test cases.
ok - CloudEvents: detect format, generate specific wrappers/attributes, integrate SDK (headers specversion/id/type/source/subject/time/dataSchema/dataContentType).
ok - Schema Registry (JSON Schema): generate `SchemaRegistryClient` bean via `multiapi.schema.registry.*` and helper to register/fetch schemas with subject strategy.
- Full Schema Registry integration (Confluent/Apicurio): configure URL/auth/SSL, choose serializer per format (JSON Schema/Avro/Protobuf), and support alternate registries (Apicurio) beyond Confluent.
ok  - Confluent: allow choosing serializer/deserializer by `schemaFormat` (JSON Schema/Avro/Protobuf) and configure URL/auth/SSL via `multiapi.schema.registry.*`.
  - Apicurio: create alternative mode (URL/auth/SSL) and compatible templates; map `schemaRegistryProvider` (confluent|apicurio).
ok  - Format-based selection: apply serializers/deserializers in supplier/consumer/streamBridge templates according to `schemaFormat`.
ok  - Client helper: adjust helper to register/fetch schemas in Confluent (JSON Schema/Avro/Protobuf) with subject naming.
ok - Channel parameters/server variables: generate @Value/environment configuration and dynamically substitute in address (e.g., xyz.{env}.topic) with profiles/properties.
ok - Channel parameters/server variables: generate @Value/environment configuration and dynamically substitute in address (e.g., xyz.{env}.topic) with profiles/properties).


## Pending items

ok - Fix `OpenApiGenerator` call in Gradle task: swapped arguments cause `MissingMethodException` when running `openApiTask` (`scs-multiapi-gradle-plugin/src/main/groovy/com/sngular/api/generator/plugin/OpenApiTask.groovy:32`).
ok - Adjust OpenAPI model generation to honor the computed package and avoid incorrect fallback (`multiapi-engine/src/main/java/com/sngular/api/generator/plugin/openapi/OpenApiGenerator.java:160-166,253-263`).
ok - Reset client/auth generation state per spec so `callMode` does not leak across files (`multiapi-engine/src/main/java/com/sngular/api/generator/plugin/openapi/OpenApiGenerator.java:59-66,81-134,168-179`).
ok - Missing support for alias/nickname different from the class name (feature present in OpenAPI) to choose generated names.
