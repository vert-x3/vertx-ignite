package io.vertx.spi.cluster.ignite;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.spi.cluster.ignite.IgniteDiscoveryOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.spi.cluster.ignite.IgniteDiscoveryOptions} original class using Vert.x codegen.
 */
public class IgniteDiscoveryOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, IgniteDiscoveryOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "type":
          if (member.getValue() instanceof String) {
            obj.setType((String)member.getValue());
          }
          break;
        case "properties":
          if (member.getValue() instanceof JsonObject) {
            obj.setProperties(((JsonObject)member.getValue()).copy());
          }
          break;
      }
    }
  }

   static void toJson(IgniteDiscoveryOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(IgniteDiscoveryOptions obj, java.util.Map<String, Object> json) {
    if (obj.getType() != null) {
      json.put("type", obj.getType());
    }
    if (obj.getProperties() != null) {
      json.put("properties", obj.getProperties());
    }
  }
}
