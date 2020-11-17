package io.vertx.spi.cluster.ignite;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.spi.cluster.ignite.IgniteOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.spi.cluster.ignite.IgniteOptions} original class using Vert.x codegen.
 */
public class IgniteOptionsConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, IgniteOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "cacheConfiguration":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<io.vertx.spi.cluster.ignite.IgniteCacheOptions> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new io.vertx.spi.cluster.ignite.IgniteCacheOptions((io.vertx.core.json.JsonObject)item));
            });
            obj.setCacheConfiguration(list);
          }
          break;
        case "connectTimeout":
          if (member.getValue() instanceof Number) {
            obj.setConnectTimeout(((Number)member.getValue()).longValue());
          }
          break;
        case "connectionsPerNode":
          if (member.getValue() instanceof Number) {
            obj.setConnectionsPerNode(((Number)member.getValue()).intValue());
          }
          break;
        case "discoverySpi":
          if (member.getValue() instanceof JsonObject) {
            obj.setDiscoverySpi(new io.vertx.spi.cluster.ignite.IgniteDiscoveryOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "idleConnectionTimeout":
          if (member.getValue() instanceof Number) {
            obj.setIdleConnectionTimeout(((Number)member.getValue()).longValue());
          }
          break;
        case "includeEventTypes":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<java.lang.String> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                list.add((String)item);
            });
            obj.setIncludeEventTypes(list);
          }
          break;
        case "localHost":
          if (member.getValue() instanceof String) {
            obj.setLocalHost((String)member.getValue());
          }
          break;
        case "localPort":
          if (member.getValue() instanceof Number) {
            obj.setLocalPort(((Number)member.getValue()).intValue());
          }
          break;
        case "maxConnectTimeout":
          if (member.getValue() instanceof Number) {
            obj.setMaxConnectTimeout(((Number)member.getValue()).longValue());
          }
          break;
        case "metricsLogFrequency":
          if (member.getValue() instanceof Number) {
            obj.setMetricsLogFrequency(((Number)member.getValue()).longValue());
          }
          break;
        case "reconnectCount":
          if (member.getValue() instanceof Number) {
            obj.setReconnectCount(((Number)member.getValue()).intValue());
          }
          break;
        case "shutdownOnSegmentation":
          if (member.getValue() instanceof Boolean) {
            obj.setShutdownOnSegmentation((Boolean)member.getValue());
          }
          break;
        case "sslContextFactory":
          if (member.getValue() instanceof JsonObject) {
            obj.setSslContextFactory(new io.vertx.spi.cluster.ignite.IgniteSslOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
      }
    }
  }

  public static void toJson(IgniteOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(IgniteOptions obj, java.util.Map<String, Object> json) {
    if (obj.getCacheConfiguration() != null) {
      JsonArray array = new JsonArray();
      obj.getCacheConfiguration().forEach(item -> array.add(item.toJson()));
      json.put("cacheConfiguration", array);
    }
    json.put("connectTimeout", obj.getConnectTimeout());
    json.put("connectionsPerNode", obj.getConnectionsPerNode());
    if (obj.getDiscoverySpi() != null) {
      json.put("discoverySpi", obj.getDiscoverySpi().toJson());
    }
    json.put("idleConnectionTimeout", obj.getIdleConnectionTimeout());
    if (obj.getIncludeEventTypes() != null) {
      JsonArray array = new JsonArray();
      obj.getIncludeEventTypes().forEach(item -> array.add(item));
      json.put("includeEventTypes", array);
    }
    if (obj.getLocalHost() != null) {
      json.put("localHost", obj.getLocalHost());
    }
    json.put("localPort", obj.getLocalPort());
    json.put("maxConnectTimeout", obj.getMaxConnectTimeout());
    json.put("metricsLogFrequency", obj.getMetricsLogFrequency());
    json.put("reconnectCount", obj.getReconnectCount());
    json.put("shutdownOnSegmentation", obj.isShutdownOnSegmentation());
    if (obj.getSslContextFactory() != null) {
      json.put("sslContextFactory", obj.getSslContextFactory().toJson());
    }
  }
}
