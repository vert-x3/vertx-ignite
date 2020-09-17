package io.vertx.spi.cluster.ignite;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.spi.cluster.ignite.IgniteSslOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.spi.cluster.ignite.IgniteSslOptions} original class using Vert.x codegen.
 */
public class IgniteSslOptionsConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, IgniteSslOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "keyAlgorithm":
          if (member.getValue() instanceof String) {
            obj.setKeyAlgorithm((String)member.getValue());
          }
          break;
        case "keyStoreFilePath":
          if (member.getValue() instanceof String) {
            obj.setKeyStoreFilePath((String)member.getValue());
          }
          break;
        case "keyStorePassword":
          if (member.getValue() instanceof String) {
            obj.setKeyStorePassword((String)member.getValue());
          }
          break;
        case "keyStoreType":
          if (member.getValue() instanceof String) {
            obj.setKeyStoreType((String)member.getValue());
          }
          break;
        case "protocol":
          if (member.getValue() instanceof String) {
            obj.setProtocol((String)member.getValue());
          }
          break;
        case "trustAll":
          if (member.getValue() instanceof Boolean) {
            obj.setTrustAll((Boolean)member.getValue());
          }
          break;
        case "trustStoreFilePath":
          if (member.getValue() instanceof String) {
            obj.setTrustStoreFilePath((String)member.getValue());
          }
          break;
        case "trustStorePassword":
          if (member.getValue() instanceof String) {
            obj.setTrustStorePassword((String)member.getValue());
          }
          break;
        case "trustStoreType":
          if (member.getValue() instanceof String) {
            obj.setTrustStoreType((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(IgniteSslOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(IgniteSslOptions obj, java.util.Map<String, Object> json) {
    if (obj.getKeyAlgorithm() != null) {
      json.put("keyAlgorithm", obj.getKeyAlgorithm());
    }
    if (obj.getKeyStoreFilePath() != null) {
      json.put("keyStoreFilePath", obj.getKeyStoreFilePath());
    }
    if (obj.getKeyStorePassword() != null) {
      json.put("keyStorePassword", obj.getKeyStorePassword());
    }
    if (obj.getKeyStoreType() != null) {
      json.put("keyStoreType", obj.getKeyStoreType());
    }
    if (obj.getProtocol() != null) {
      json.put("protocol", obj.getProtocol());
    }
    json.put("trustAll", obj.isTrustAll());
    if (obj.getTrustStoreFilePath() != null) {
      json.put("trustStoreFilePath", obj.getTrustStoreFilePath());
    }
    if (obj.getTrustStorePassword() != null) {
      json.put("trustStorePassword", obj.getTrustStorePassword());
    }
    if (obj.getTrustStoreType() != null) {
      json.put("trustStoreType", obj.getTrustStoreType());
    }
  }
}
