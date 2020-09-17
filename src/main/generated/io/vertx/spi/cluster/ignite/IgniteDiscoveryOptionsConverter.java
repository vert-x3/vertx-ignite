package io.vertx.spi.cluster.ignite;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.spi.cluster.ignite.IgniteDiscoveryOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.spi.cluster.ignite.IgniteDiscoveryOptions} original class using Vert.x codegen.
 */
public class IgniteDiscoveryOptionsConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, IgniteDiscoveryOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "properties":
          if (member.getValue() instanceof JsonObject) {
            obj.setProperties(((JsonObject)member.getValue()).copy());
          }
          break;
        case "type":
          if (member.getValue() instanceof String) {
            obj.setType((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(IgniteDiscoveryOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(IgniteDiscoveryOptions obj, java.util.Map<String, Object> json) {
    if (obj.getProperties() != null) {
      json.put("properties", obj.getProperties());
    }
    if (obj.getType() != null) {
      json.put("type", obj.getType());
    }
  }
}
