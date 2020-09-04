package io.vertx.spi.cluster.ignite.util;

import io.vertx.core.VertxException;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.ignite.IgniteOptions;
import io.vertx.spi.cluster.ignite.impl.VertxLogger;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.util.typedef.F;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class ConfigHelper {

  private ConfigHelper() {
    //private
  }

  public static IgniteConfiguration loadConfiguration(URL config) {
    try {
      return F.first(IgnitionEx.loadConfigurations(config).get1());
    } catch (IgniteCheckedException e) {
      throw new VertxException(e);
    }
  }

  public static IgniteConfiguration loadConfiguration(JsonObject json) {
    IgniteOptions options = new IgniteOptions(json);
    IgniteConfiguration config = options.toConfig();
    config.setGridLogger(new VertxLogger());
    return config;
  }

  public static IgniteConfiguration lookupXmlConfiguration(Class<?> clazz, String... files) {
    InputStream is = lookupFiles(clazz, files);
    try {
      return F.first(IgnitionEx.loadConfigurations(is).get1());
    } catch (IgniteCheckedException | IllegalArgumentException e) {
      throw new VertxException(e);
    }
  }

  public static JsonObject lookupJsonConfiguration(Class<?> clazz, String... files) {
    InputStream is = lookupFiles(clazz, files);
    try {
      return new JsonObject(readFromInputStream(is));
    } catch (NullPointerException | DecodeException | IOException e) {
      throw new VertxException(e);
    }
  }

  public static InputStream lookupFiles(Class<?> clazz, String... files) {
    ClassLoader ctxClsLoader = Thread.currentThread().getContextClassLoader();
    InputStream is = null;
    for (String file : files) {
      if (ctxClsLoader != null) {
        is = ctxClsLoader.getResourceAsStream(file);
      }
      if (is == null) {
        is = clazz.getClassLoader().getResourceAsStream(file);
      }
      if (is != null) {
        break;
      }
    }
    return is;
  }

  private static String readFromInputStream(InputStream inputStream) throws IOException {
    StringBuilder resultStringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
  }
}
