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

  /**
   * Default constructor
   */
  public IgniteSslOptions() {
    protocol = DFLT_SSL_PROTOCOL;
    keyAlgorithm = DFLT_KEY_ALGORITHM;
    keyStoreType = DFLT_STORE_TYPE;
    trustStoreType = DFLT_STORE_TYPE;
    trustAll = false;
  }

  /**
   * Copy constructor
   *
   * @param options the one to copy
   */
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

  /**
   * Constructor from JSON
   *
   * @param options the JSON
   */
  public IgniteSslOptions(JsonObject options) {
    this();
    IgniteSslOptionsConverter.fromJson(options, this);
  }

  /**
   * Gets protocol for secure transport.
   *
   * @return SSL protocol name.
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * Sets protocol for secure transport.
   *
   * @param protocol SSL protocol name.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  /**
   * Gets algorithm that will be used to create a key manager.
   *
   * @return Key manager algorithm.
   */
  public String getKeyAlgorithm() {
    return keyAlgorithm;
  }

  /**
   * Sets key manager algorithm that will be used to create a key manager. Notice that in most cased default value
   * suites well, however, on Android platform this value need to be set to <tt>X509<tt/>.
   *
   * @param keyAlgorithm Key algorithm name.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setKeyAlgorithm(String keyAlgorithm) {
    this.keyAlgorithm = keyAlgorithm;
    return this;
  }

  /**
   * Gets key store type used for context creation.
   *
   * @return Key store type.
   */
  public String getKeyStoreType() {
    return keyStoreType;
  }

  /**
   * Sets key store type used in context initialization.
   *
   * @param keyStoreType Key store type.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setKeyStoreType(String keyStoreType) {
    this.keyStoreType = keyStoreType;
    return this;
  }

  /**
   * Gets path to the key store file.
   *
   * @return Path to key store file.
   */
  public String getKeyStoreFilePath() {
    return keyStoreFilePath;
  }

  /**
   * Sets path to the key store file. This is a mandatory parameter since
   * ssl context could not be initialized without key manager.
   *
   * @param keyStoreFilePath Path to key store file.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setKeyStoreFilePath(String keyStoreFilePath) {
    this.keyStoreFilePath = keyStoreFilePath;
    return this;
  }

  /**
   * Gets key store password.
   *
   * @return Key store password.
   */
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  /**
   * Sets key store password.
   *
   * @param keyStorePassword Key store password.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
    return this;
  }

  /**
   * Gets trust store type used for context creation.
   *
   * @return trust store type.
   */
  public String getTrustStoreType() {
    return trustStoreType;
  }

  /**
   * Sets trust store type used in context initialization.
   *
   * @param trustStoreType Trust store type.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setTrustStoreType(String trustStoreType) {
    this.trustStoreType = trustStoreType;
    return this;
  }

  /**
   * Gets path to the trust store file.
   *
   * @return Path to the trust store file.
   */
  public String getTrustStoreFilePath() {
    return trustStoreFilePath;
  }

  /**
   * Sets path to the trust store file.
   *
   * @param trustStoreFilePath Path to the trust store file.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setTrustStoreFilePath(String trustStoreFilePath) {
    this.trustStoreFilePath = trustStoreFilePath;
    return this;
  }

  /**
   * Gets trust store password.
   *
   * @return Trust store password.
   */
  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  /**
   * Sets trust store password.
   *
   * @param trustStorePassword Trust store password.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setTrustStorePassword(String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
    return this;
  }

  /**
   * When using ssl, trust ALL certificates.
   * WARNING Trusting ALL certificates will open you up to potential security issues such as MITM attacks.
   *
   * @return Trust all flag.
   */
  public boolean isTrustAll() {
    return trustAll;
  }

  /**
   * When using ssl, trust ALL certificates.
   * WARNING Trusting ALL certificates will open you up to potential security issues such as MITM attacks.
   *
   * @param trustAll Trust all flag.
   * @return reference to this, for fluency
   */
  public IgniteSslOptions setTrustAll(boolean trustAll) {
    this.trustAll = trustAll;
    return this;
  }

  /**
   * Convert to JSON
   *
   * @return the JSON
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    IgniteSslOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * Convert to IgniteConfiguration
   *
   * @return the SslContextFactory
   */
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
