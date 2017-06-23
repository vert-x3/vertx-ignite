package io.vertx.spi.cluster.ignite.cfg;

import java.io.InputStream;

import org.apache.ignite.configuration.IgniteConfiguration;

import io.vertx.core.json.JsonObject;

public interface IgniteConfigurationLoader {

  IgniteConfiguration fromJson(JsonObject json);
  IgniteConfiguration fromJson(String json);
  IgniteConfiguration fromJson(InputStream is);
  
}
