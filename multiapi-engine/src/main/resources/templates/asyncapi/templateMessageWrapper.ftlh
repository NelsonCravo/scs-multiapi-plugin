package ${wrapperPackage};

import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.v1.CloudEventBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

public class MessageWrapper<T, Y> {

  private T payload;

  private Y key;

  private String correlationId;

  private String replyTo;

  private String ceSpecVersion;

  private String ceId;

  private String ceType;

  private String ceSource;

  private String ceSubject;

  private String ceTime;

  private String ceDataSchema;

  private String ceDataContentType;

  private MessageWrapper(final Y key, final T payload) {
    this.payload = payload;
    this.key = key;
  }

  private MessageWrapper(final Y key, final T payload, final String correlationId, final String replyTo, final String ceSpecVersion, final String ceId,
      final String ceType, final String ceSource, final String ceSubject, final String ceTime, final String ceDataSchema, final String ceDataContentType) {
    this.payload = payload;
    this.key = key;
    this.correlationId = correlationId;
    this.replyTo = replyTo;
    this.ceSpecVersion = ceSpecVersion;
    this.ceId = ceId;
    this.ceType = ceType;
    this.ceSource = ceSource;
    this.ceSubject = ceSubject;
    this.ceTime = ceTime;
    this.ceDataSchema = ceDataSchema;
    this.ceDataContentType = ceDataContentType;
  }

  public T getPayload() {
   return payload;
  }

  public void setPayload(T payload) {
    this.payload = payload;
  }

  public Y getKey() {
    return key;
  }

  public void setKey(Y key) {
    this.key = key;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(final String correlationId) {
    this.correlationId = correlationId;
  }

  public String getReplyTo() {
    return replyTo;
  }

  public void setReplyTo(final String replyTo) {
    this.replyTo = replyTo;
  }

  public String getCeSpecVersion() {
    return ceSpecVersion;
  }

  public void setCeSpecVersion(final String ceSpecVersion) {
    this.ceSpecVersion = ceSpecVersion;
  }

  public String getCeId() {
    return ceId;
  }

  public void setCeId(final String ceId) {
    this.ceId = ceId;
  }

  public String getCeType() {
    return ceType;
  }

  public void setCeType(final String ceType) {
    this.ceType = ceType;
  }

  public String getCeSource() {
    return ceSource;
  }

  public void setCeSource(final String ceSource) {
    this.ceSource = ceSource;
  }

  public String getCeSubject() {
    return ceSubject;
  }

  public void setCeSubject(final String ceSubject) {
    this.ceSubject = ceSubject;
  }

  public String getCeTime() {
    return ceTime;
  }

  public void setCeTime(final String ceTime) {
    this.ceTime = ceTime;
  }

  public String getCeDataSchema() {
    return ceDataSchema;
  }

  public void setCeDataSchema(final String ceDataSchema) {
    this.ceDataSchema = ceDataSchema;
  }

  public String getCeDataContentType() {
    return ceDataContentType;
  }

  public void setCeDataContentType(final String ceDataContentType) {
    this.ceDataContentType = ceDataContentType;
  }

  public static MessageWrapperBuilder builder() {
    return new MessageWrapperBuilder();
  }

  public static class MessageWrapperBuilder<T, Y> {

    private T payload;

    private Y key;

    private String correlationId;

    private String replyTo;

    private String ceSpecVersion;

    private String ceId;

    private String ceType;

    private String ceSource;

    private String ceSubject;

    private String ceTime;

    private String ceDataSchema;

    private String ceDataContentType;

    public MessageWrapperBuilder payload(final T payload) {
      this.payload = payload;
      return this;
    }

    public MessageWrapperBuilder key(final Y key) {
      this.key = key;
      return this;
    }

    public MessageWrapperBuilder correlationId(final String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public MessageWrapperBuilder replyTo(final String replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public MessageWrapperBuilder ceSpecVersion(final String ceSpecVersion) {
      this.ceSpecVersion = ceSpecVersion;
      return this;
    }

    public MessageWrapperBuilder ceId(final String ceId) {
      this.ceId = ceId;
      return this;
    }

    public MessageWrapperBuilder ceType(final String ceType) {
      this.ceType = ceType;
      return this;
    }

    public MessageWrapperBuilder ceSource(final String ceSource) {
      this.ceSource = ceSource;
      return this;
    }

    public MessageWrapperBuilder ceSubject(final String ceSubject) {
      this.ceSubject = ceSubject;
      return this;
    }

    public MessageWrapperBuilder ceTime(final String ceTime) {
      this.ceTime = ceTime;
      return this;
    }

    public MessageWrapperBuilder ceDataSchema(final String ceDataSchema) {
      this.ceDataSchema = ceDataSchema;
      return this;
    }

  public MessageWrapperBuilder ceDataContentType(final String ceDataContentType) {
      this.ceDataContentType = ceDataContentType;
      return this;
    }

    public MessageWrapper build() {
      return new MessageWrapper(key, payload, correlationId, replyTo, ceSpecVersion, ceId, ceType, ceSource, ceSubject, ceTime, ceDataSchema, ceDataContentType);
    }
  }

  public CloudEvent toCloudEvent() {
    final var builder = CloudEventBuilder.v1()
                                         .withId(ceId != null ? ceId : correlationId != null ? correlationId : UUID.randomUUID().toString())
                                         .withSource(URI.create(ceSource != null ? ceSource : "urn:default"))
                                         .withType(ceType != null ? ceType : "event");
    if (ceSpecVersion != null && !"1.0".equals(ceSpecVersion)) {
      builder.withSpecVersion(io.cloudevents.SpecVersion.valueOf(ceSpecVersion));
    }
    if (ceSubject != null) {
      builder.withSubject(ceSubject);
    }
    if (ceDataSchema != null) {
      builder.withDataSchema(URI.create(ceDataSchema));
    }
    if (ceDataContentType != null) {
      builder.withDataContentType(ceDataContentType);
    }
    if (ceTime != null) {
      try {
        builder.withTime(OffsetDateTime.parse(ceTime));
      } catch (final Exception ignored) {
      }
    }
    if (payload != null) {
      final byte[] data = payload instanceof byte[] ? (byte[]) payload : payload.toString().getBytes(StandardCharsets.UTF_8);
      builder.withData(BytesCloudEventData.wrap(data));
    }
    return builder.build();
  }

  public static <T, Y> MessageWrapper<T, Y> fromCloudEvent(final CloudEvent event, final Y key, final T payload) {
    return MessageWrapper.<T, Y>builder()
                         .payload(payload)
                         .key(key)
                         .correlationId(event.getId())
                         .ceId(event.getId())
                         .ceSpecVersion(event.getSpecVersion() != null ? event.getSpecVersion().toString() : "1.0")
                         .ceType(event.getType())
                         .ceSource(event.getSource() != null ? event.getSource().toString() : null)
                         .ceSubject(event.getSubject())
                         .ceTime(event.getTime() != null ? event.getTime().toString() : null)
                         .ceDataSchema(event.getDataSchema() != null ? event.getDataSchema().toString() : null)
                         .ceDataContentType(event.getDataContentType())
                         .build();
  }
}
