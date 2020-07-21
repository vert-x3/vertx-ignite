/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.vertx.spi.cluster.ignite;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.apache.ignite.ssl.SslContextFactory;

import static org.apache.ignite.ssl.SslContextFactory.*;

/**
 * @author Lukas Prettenthaler
 */
@DataObject(generateConverter = true)
public class IgniteSslOptions {
  private String protocol;
  private String keyAlgorithm;
  private String keyStoreType;
  private String keyStoreFilePath;
  private String keyStorePassword;
  private String trustStoreType;
  private String trustStoreFilePath;
  private String trustStorePassword;
  private boolean trustAll;

  public IgniteSslOptions() {
    protocol = DFLT_SSL_PROTOCOL;
    keyAlgorithm = DFLT_KEY_ALGORITHM;
    keyStoreType = DFLT_STORE_TYPE;
    trustStoreType = DFLT_STORE_TYPE;
    trustAll = false;
  }

  public IgniteSslOptions(IgniteSslOptions options) {
    this.protocol = options.protocol;
    this.keyAlgorithm = options.keyAlgorithm;
    this.keyStoreType = options.keyStoreType;
    this.keyStoreFilePath = options.keyStoreFilePath;
    this.keyStorePassword = options.keyStorePassword;
    this.trustStoreType = options.trustStoreType;
    this.trustStoreFilePath = options.trustStoreFilePath;
    this.trustStorePassword = options.trustStorePassword;
    this.trustAll = options.trustAll;
  }

  public IgniteSslOptions(JsonObject options) {
    this();
    IgniteSslOptionsConverter.fromJson(options, this);
  }

  public String getProtocol() {
    return protocol;
  }

  public IgniteSslOptions setProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public String getKeyAlgorithm() {
    return keyAlgorithm;
  }

  public IgniteSslOptions setKeyAlgorithm(String keyAlgorithm) {
    this.keyAlgorithm = keyAlgorithm;
    return this;
  }

  public String getKeyStoreType() {
    return keyStoreType;
  }

  public IgniteSslOptions setKeyStoreType(String keyStoreType) {
    this.keyStoreType = keyStoreType;
    return this;
  }

  public String getKeyStoreFilePath() {
    return keyStoreFilePath;
  }

  public IgniteSslOptions setKeyStoreFilePath(String keyStoreFilePath) {
    this.keyStoreFilePath = keyStoreFilePath;
    return this;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public IgniteSslOptions setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
    return this;
  }

  public String getTrustStoreType() {
    return trustStoreType;
  }

  public IgniteSslOptions setTrustStoreType(String trustStoreType) {
    this.trustStoreType = trustStoreType;
    return this;
  }

  public String getTrustStoreFilePath() {
    return trustStoreFilePath;
  }

  public IgniteSslOptions setTrustStoreFilePath(String trustStoreFilePath) {
    this.trustStoreFilePath = trustStoreFilePath;
    return this;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public IgniteSslOptions setTrustStorePassword(String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
    return this;
  }

  public boolean isTrustAll() {
    return trustAll;
  }

  public IgniteSslOptions setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    IgniteSslOptionsConverter.toJson(this, json);
    return json;
  }

  public SslContextFactory toConfig() {
    if(keyStoreFilePath == null || keyStoreFilePath.isEmpty()) {
      return null;
    }
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setProtocol(protocol);
    sslContextFactory.setKeyAlgorithm(keyAlgorithm);
    sslContextFactory.setKeyStoreType(keyStoreType);
    sslContextFactory.setKeyStoreFilePath(keyStoreFilePath);
    if(keyStorePassword != null) {
      sslContextFactory.setKeyStorePassword(keyStorePassword.toCharArray());
    }
    sslContextFactory.setTrustStoreType(trustStoreType);
    sslContextFactory.setTrustStoreFilePath(trustStoreFilePath);
    if(trustStorePassword != null) {
      sslContextFactory.setTrustStorePassword(trustStorePassword.toCharArray());
    }
    if(trustAll) {
      sslContextFactory.setTrustManagers(getDisabledTrustManager());
    }
    return sslContextFactory;
  }
}
