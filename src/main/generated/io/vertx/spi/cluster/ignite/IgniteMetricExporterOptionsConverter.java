package io.vertx.spi.cluster.ignite;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.spi.cluster.ignite.IgniteMetricExporterOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.spi.cluster.ignite.IgniteMetricExporterOptions} original class using Vert.x codegen.
 */
public class IgniteMetricExporterOptionsConverter {

  private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
  private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

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
