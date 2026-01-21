package ${supplierPackage};

import java.util.Map;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchemaRegistryConfig {

  @Value("${r"${multiapi.schema.registry.url:}"}")
  private String schemaRegistryUrl;

  @Value("${r"${multiapi.schema.registry.max-cached-schemas:1000}"}")
  private int maxCachedSchemas;

  @Value("${r"${multiapi.schema.registry.basic-auth:}"}")
  private String basicAuth;

  @Bean
  public SchemaRegistryClient schemaRegistryClient() {
    if (schemaRegistryUrl == null || schemaRegistryUrl.isBlank()) {
      throw new IllegalStateException(
          "Schema Registry URL not configured (multiapi.schema.registry.url)"
      );
    }
    final Map<String, String> clientConfig = buildClientConfig();
    return new CachedSchemaRegistryClient(
        schemaRegistryUrl,
        maxCachedSchemas,
        clientConfig
    );
  }

  private Map<String, String> buildClientConfig() {
    if (basicAuth != null && !basicAuth.isBlank()) {
      return Map.of(
          "basic.auth.credentials.source", "USER_INFO",
          "basic.auth.user.info", basicAuth
      );
    }
    return Map.of();
  }

  @Bean
  public SchemaRegistryHelper schemaRegistryHelper(final SchemaRegistryClient client) {
    return new SchemaRegistryHelper(client);
  }

  public static class SchemaRegistryHelper {

    private final SchemaRegistryClient client;

    public SchemaRegistryHelper(final SchemaRegistryClient client) {
      this.client = client;
    }

    public int registerJsonSchema(final String subject, final String schemaJson) {
      try {
        return client.register(subject, new JsonSchema(schemaJson));
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Could not register JSON schema for subject " + subject,
            e
        );
      }
    }

    public JsonSchema fetchLatestJsonSchema(final String subject) {
      try {
        final var metadata = client.getLatestSchemaMetadata(subject);
        return new JsonSchema(metadata.getSchema());
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Could not fetch latest JSON schema for subject " + subject,
            e
        );
      }
    }

    public int registerAvroSchema(final String subject, final String avroSchema) {
      try {
        return client.register(subject, new AvroSchema(avroSchema));
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Could not register Avro schema for subject " + subject,
            e
        );
      }
    }

    public AvroSchema fetchLatestAvroSchema(final String subject) {
      try {
        final var metadata = client.getLatestSchemaMetadata(subject);
        return new AvroSchema(metadata.getSchema());
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Could not fetch latest Avro schema for subject " + subject,
            e
        );
      }
    }

    public int registerProtobufSchema(final String subject, final String protoSchema) {
      try {
        return client.register(subject, new ProtobufSchema(protoSchema));
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Could not register Protobuf schema for subject " + subject,
            e
        );
      }
    }

    public ProtobufSchema fetchLatestProtobufSchema(final String subject) {
      try {
        final var metadata = client.getLatestSchemaMetadata(subject);
        return new ProtobufSchema(metadata.getSchema());
      } catch (final Exception e) {
        throw new IllegalStateException(
            "Could not fetch latest Protobuf schema for subject " + subject,
            e
        );
      }
    }

    public String subjectFor(final String topic, final boolean key) {
      if (topic == null || topic.isBlank()) {
        throw new IllegalArgumentException("topic must not be blank");
      }
      return topic + "-" + (key ? "key" : "value");
    }
  }
}
