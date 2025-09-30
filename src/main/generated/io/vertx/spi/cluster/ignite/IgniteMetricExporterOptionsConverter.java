package io.vertx.spi.cluster.ignite;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter and mapper for {@link io.vertx.spi.cluster.ignite.IgniteMetricExporterOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.spi.cluster.ignite.IgniteMetricExporterOptions} original class using Vert.x codegen.
 */
public class IgniteMetricExporterOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, IgniteMetricExporterOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
      }
    }
  }

   static void toJson(IgniteMetricExporterOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(IgniteMetricExporterOptions obj, java.util.Map<String, Object> json) {
  }
}
