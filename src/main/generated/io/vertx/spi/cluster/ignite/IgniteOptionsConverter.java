package io.vertx.spi.cluster.ignite;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.spi.cluster.ignite.IgniteOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.spi.cluster.ignite.IgniteOptions} original class using Vert.x codegen.
 */
public class IgniteOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, IgniteOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
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
        case "connectionsPerNode":
          if (member.getValue() instanceof Number) {
            obj.setConnectionsPerNode(((Number)member.getValue()).intValue());
          }
          break;
        case "connectTimeout":
          if (member.getValue() instanceof Number) {
            obj.setConnectTimeout(((Number)member.getValue()).longValue());
          }
          break;
        case "idleConnectionTimeout":
          if (member.getValue() instanceof Number) {
            obj.setIdleConnectionTimeout(((Number)member.getValue()).longValue());
          }
          break;
        case "maxConnectTimeout":
          if (member.getValue() instanceof Number) {
            obj.setMaxConnectTimeout(((Number)member.getValue()).longValue());
          }
          break;
        case "reconnectCount":
          if (member.getValue() instanceof Number) {
            obj.setReconnectCount(((Number)member.getValue()).intValue());
          }
          break;
        case "metricsLogFrequency":
          if (member.getValue() instanceof Number) {
            obj.setMetricsLogFrequency(((Number)member.getValue()).longValue());
          }
          break;
        case "discoverySpi":
          if (member.getValue() instanceof JsonObject) {
            obj.setDiscoverySpi(new io.vertx.spi.cluster.ignite.IgniteDiscoveryOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
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
        case "sslContextFactory":
          if (member.getValue() instanceof JsonObject) {
            obj.setSslContextFactory(new io.vertx.spi.cluster.ignite.IgniteSslOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "shutdownOnSegmentation":
          if (member.getValue() instanceof Boolean) {
            obj.setShutdownOnSegmentation((Boolean)member.getValue());
          }
          break;
        case "pageSize":
          if (member.getValue() instanceof Number) {
            obj.setPageSize(((Number)member.getValue()).intValue());
          }
          break;
        case "defaultRegionInitialSize":
          if (member.getValue() instanceof Number) {
            obj.setDefaultRegionInitialSize(((Number)member.getValue()).longValue());
          }
          break;
        case "defaultRegionMaxSize":
          if (member.getValue() instanceof Number) {
            obj.setDefaultRegionMaxSize(((Number)member.getValue()).longValue());
          }
          break;
        case "defaultRegionMetricsEnabled":
          if (member.getValue() instanceof Boolean) {
            obj.setDefaultRegionMetricsEnabled((Boolean)member.getValue());
          }
          break;
        case "shutdownOnNodeStop":
          if (member.getValue() instanceof Boolean) {
            obj.setShutdownOnNodeStop((Boolean)member.getValue());
          }
          break;
        case "metricsUpdateFrequency":
          if (member.getValue() instanceof Number) {
            obj.setMetricsUpdateFrequency(((Number)member.getValue()).longValue());
          }
          break;
        case "clientFailureDetectionTimeout":
          if (member.getValue() instanceof Number) {
            obj.setClientFailureDetectionTimeout(((Number)member.getValue()).longValue());
          }
          break;
        case "metricsHistorySize":
          if (member.getValue() instanceof Number) {
            obj.setMetricsHistorySize(((Number)member.getValue()).intValue());
          }
          break;
        case "metricsExpireTime":
          if (member.getValue() instanceof Number) {
            obj.setMetricsExpireTime(((Number)member.getValue()).longValue());
          }
          break;
        case "metricExporterSpi":
          if (member.getValue() instanceof JsonObject) {
            obj.setMetricExporterSpi(new io.vertx.spi.cluster.ignite.IgniteMetricExporterOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "delayAfterStart":
          if (member.getValue() instanceof Number) {
            obj.setDelayAfterStart(((Number)member.getValue()).longValue());
          }
          break;
        case "dataPageEvictionMode":
          if (member.getValue() instanceof String) {
            obj.setDataPageEvictionMode((String)member.getValue());
          }
          break;
      }
    }
  }

   static void toJson(IgniteOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(IgniteOptions obj, java.util.Map<String, Object> json) {
    if (obj.getLocalHost() != null) {
      json.put("localHost", obj.getLocalHost());
    }
    json.put("localPort", obj.getLocalPort());
    json.put("connectionsPerNode", obj.getConnectionsPerNode());
    json.put("connectTimeout", obj.getConnectTimeout());
    json.put("idleConnectionTimeout", obj.getIdleConnectionTimeout());
    json.put("maxConnectTimeout", obj.getMaxConnectTimeout());
    json.put("reconnectCount", obj.getReconnectCount());
    json.put("metricsLogFrequency", obj.getMetricsLogFrequency());
    if (obj.getDiscoverySpi() != null) {
      json.put("discoverySpi", obj.getDiscoverySpi().toJson());
    }
    if (obj.getCacheConfiguration() != null) {
      JsonArray array = new JsonArray();
      obj.getCacheConfiguration().forEach(item -> array.add(item.toJson()));
      json.put("cacheConfiguration", array);
    }
    if (obj.getSslContextFactory() != null) {
      json.put("sslContextFactory", obj.getSslContextFactory().toJson());
    }
    json.put("shutdownOnSegmentation", obj.isShutdownOnSegmentation());
    json.put("pageSize", obj.getPageSize());
    json.put("defaultRegionInitialSize", obj.getDefaultRegionInitialSize());
    json.put("defaultRegionMaxSize", obj.getDefaultRegionMaxSize());
    json.put("defaultRegionMetricsEnabled", obj.isDefaultRegionMetricsEnabled());
    json.put("shutdownOnNodeStop", obj.isShutdownOnNodeStop());
    json.put("metricsUpdateFrequency", obj.getMetricsUpdateFrequency());
    json.put("clientFailureDetectionTimeout", obj.getClientFailureDetectionTimeout());
    json.put("metricsHistorySize", obj.getMetricsHistorySize());
    json.put("metricsExpireTime", obj.getMetricsExpireTime());
    if (obj.getMetricExporterSpi() != null) {
      json.put("metricExporterSpi", obj.getMetricExporterSpi().toJson());
    }
    json.put("delayAfterStart", obj.getDelayAfterStart());
    if (obj.getDataPageEvictionMode() != null) {
      json.put("dataPageEvictionMode", obj.getDataPageEvictionMode());
    }
  }
}
