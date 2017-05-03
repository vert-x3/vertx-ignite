package io.vertx.spi.cluster.ignite.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.ignite.configuration.IgniteConfiguration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.ignite.cfg.IgniteConfigurationLoader;
import io.vertx.spi.cluster.ignite.cfg.IgniteConfigurationMixin;

public class IgniteConfigurationLoaderImpl implements IgniteConfigurationLoader {

  private final ObjectMapper mapper;

  public IgniteConfigurationLoaderImpl() {
    this.mapper = new ObjectMapper();
    mapper.addMixIn(IgniteConfiguration.class, IgniteConfigurationMixin.class);
    mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.NONE).withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    StdTypeResolverBuilder typeResolverBuilder = new ObjectMapper.DefaultTypeResolverBuilder(
        DefaultTyping.NON_CONCRETE_AND_ARRAYS) {
      @Override
      public boolean useForType(JavaType t) {
        while(t.isCollectionLikeType()) {
          t = t.getContentType();
        }
        while (t.isArrayType()) {
          t = t.getContentType();
        }
        // 19-Apr-2016, tatu: ReferenceType like Optional also requires similar
        // handling:
        while (t.isReferenceType()) {
          t = t.getReferencedType();
        }
        return t.isJavaLangObject() || (!t.isConcrete()
            // [databind#88] Should not apply to JSON tree models:
            && !TreeNode.class.isAssignableFrom(t.getRawClass()));
      }
    };
    typeResolverBuilder = typeResolverBuilder.init(JsonTypeInfo.Id.CLASS, null).inclusion(JsonTypeInfo.As.PROPERTY)
        .typeProperty("@class");
    mapper.setDefaultTyping(typeResolverBuilder);
    mapper.disable(MapperFeature.USE_GETTERS_AS_SETTERS);
  }

  @Override
  public IgniteConfiguration fromJson(JsonObject json) {
    String jsonStr = json.encode();
    return fromJson(jsonStr);
  }

  @Override
  public IgniteConfiguration fromJson(String jsonStr) {
    IgniteConfiguration cfg;
    try {
      cfg = mapper.readValue(jsonStr, IgniteConfiguration.class);
      return cfg;
    } catch (IOException e) {
      throw new DecodeException("Failed to decode:" + e.getMessage(), e);
    }
  }

  @Override
  public IgniteConfiguration fromJson(InputStream is) {
    IgniteConfiguration cfg;
    try {
      cfg = mapper.readValue(new InputStreamReader(is, "UTF-8"), IgniteConfiguration.class);
      return cfg;
    } catch (IOException e) {
      throw new DecodeException("Failed to decode:" + e.getMessage(), e);
    }
  }

}
