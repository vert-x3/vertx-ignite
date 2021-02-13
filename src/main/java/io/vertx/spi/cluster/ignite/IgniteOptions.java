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

import java.util.*;

import static org.apache.ignite.configuration.DataStorageConfiguration.*;
import static org.apache.ignite.configuration.IgniteConfiguration.DFLT_METRICS_LOG_FREQ;
import static org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi.*;

/**
 * @author Lukas Prettenthaler
 */
@DataObject(generateConverter = true)
public class IgniteOptions {
  private String localHost;
  private int localPort;
  private int connectionsPerNode;
  private long connectTimeout;
  private long idleConnectionTimeout;
  private long maxConnectTimeout;
  private int reconnectCount;
  private long metricsLogFrequency;
  private IgniteDiscoveryOptions discoveryOptions;
  private List<IgniteCacheOptions> cacheConfiguration;
  private IgniteSslOptions sslOptions;
  private boolean shutdownOnSegmentation;
  private long defaultRegionInitialSize;
  private long defaultRegionMaxSize;

  /**
   * Default constructor
   */
  public IgniteOptions() {
    localPort = DFLT_PORT;
    connectionsPerNode = DFLT_CONN_PER_NODE;
    connectTimeout = DFLT_CONN_TIMEOUT;
    idleConnectionTimeout = DFLT_IDLE_CONN_TIMEOUT;
    reconnectCount = DFLT_RECONNECT_CNT;
    maxConnectTimeout = DFLT_MAX_CONN_TIMEOUT;
    metricsLogFrequency = DFLT_METRICS_LOG_FREQ;
    discoveryOptions = new IgniteDiscoveryOptions();
    cacheConfiguration = new ArrayList<>();
    shutdownOnSegmentation = true;
    defaultRegionInitialSize = DFLT_DATA_REGION_INITIAL_SIZE;
    defaultRegionMaxSize = DFLT_DATA_REGION_MAX_SIZE;
  }

  /**
   * Copy constructor
   *
   * @param options the one to copy
   */
  public IgniteOptions(IgniteOptions options) {
    this.localHost = options.localHost;
    this.localPort = options.localPort;
    this.connectionsPerNode = options.connectionsPerNode;
    this.connectTimeout = options.connectTimeout;
    this.idleConnectionTimeout = options.idleConnectionTimeout;
    this.reconnectCount = options.reconnectCount;
    this.maxConnectTimeout = options.maxConnectTimeout;
    this.metricsLogFrequency = options.metricsLogFrequency;
    this.discoveryOptions = options.discoveryOptions;
    this.cacheConfiguration = options.cacheConfiguration;
    this.sslOptions = options.sslOptions;
    this.shutdownOnSegmentation = options.shutdownOnSegmentation;
    this.defaultRegionInitialSize = options.defaultRegionInitialSize;
    this.defaultRegionMaxSize = options.defaultRegionMaxSize;
  }

  /**
   * Constructor from JSON
   *
   * @param options the JSON
   */
  public IgniteOptions(JsonObject options) {
    this();
    IgniteOptionsConverter.fromJson(options, this);
  }

  /**
   * Gets system-wide local address or host for all Ignite components to bind to. If provided it will
   * override all default local bind settings within Ignite or any of its SPIs.
   *
   * @return Local address or host to bind to.
   */
  public String getLocalHost() {
    return localHost;
  }

  /**
   * Sets system-wide local address or host for all Ignite components to bind to. If provided it will
   * override all default local bind settings within Ignite or any of its SPIs.
   *
   * @param localHost Local IP address or host to bind to.
   * @return reference to this, for fluency
   */
  public IgniteOptions setLocalHost(String localHost) {
    this.localHost = localHost;
    return this;
  }

  /**
   * See {@link #setLocalPort(int)}.
   *
   * @return Port number.
   */
  public int getLocalPort() {
    return localPort;
  }

  /**
   * Sets local port for socket binding.
   *
   * @param localPort Port number.
   * @return reference to this, for fluency
   */
  public IgniteOptions setLocalPort(int localPort) {
    this.localPort = localPort;
    return this;
  }

  /**
   * See {@link #setConnectionsPerNode(int)}.
   *
   * @return Number of connections per node.
   */
  public int getConnectionsPerNode() {
    return connectionsPerNode;
  }

  /**
   * Sets number of connections to each remote node.
   *
   * @param connectionsPerNode Number of connections per node.
   * @return reference to this, for fluency
   */
  public IgniteOptions setConnectionsPerNode(int connectionsPerNode) {
    this.connectionsPerNode = connectionsPerNode;
    return this;
  }

  /**
   * See {@link #setConnectTimeout(long)}.
   *
   * @return Connect timeout.
   */
  public long getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Sets connect timeout used when establishing connection
   * with remote nodes.
   *
   * @param connectTimeout Connect timeout.
   * @return reference to this, for fluency
   */
  public IgniteOptions setConnectTimeout(long connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  /**
   * See {@link #setIdleConnectionTimeout(long)}.
   *
   * @return Maximum idle connection time.
   */
  public long getIdleConnectionTimeout() {
    return idleConnectionTimeout;
  }

  /**
   * Sets maximum idle connection timeout upon which a connection
   * to client will be closed.
   *
   * @param idleConnectionTimeout Maximum idle connection time.
   * @return reference to this, for fluency
   */
  public IgniteOptions setIdleConnectionTimeout(long idleConnectionTimeout) {
    this.idleConnectionTimeout = idleConnectionTimeout;
    return this;
  }

  /**
   * See {@link #setConnectTimeout(long)}.
   *
   * @return Connect timeout.
   */
  public long getMaxConnectTimeout() {
    return maxConnectTimeout;
  }

  /**
   * Sets maximum connect timeout. If handshake is not established within connect timeout,
   * then SPI tries to repeat handshake procedure with increased connect timeout.
   * Connect timeout can grow till maximum timeout value,
   * if maximum timeout value is reached then the handshake is considered as failed.
   *
   * @param maxConnectTimeout Maximum connect timeout.
   * @return reference to this, for fluency
   */
  public IgniteOptions setMaxConnectTimeout(long maxConnectTimeout) {
    this.maxConnectTimeout = maxConnectTimeout;
    return this;
  }

  /**
   * Gets maximum number of reconnect attempts used when establishing connection
   * with remote nodes.
   *
   * @return Reconnects count.
   */
  public int getReconnectCount() {
    return reconnectCount;
  }

  /**
   * Sets maximum number of reconnect attempts used when establishing connection
   * with remote nodes.
   *
   * @param reconnectCount Maximum number of reconnection attempts.
   * @return reference to this, for fluency
   */
  public IgniteOptions setReconnectCount(int reconnectCount) {
    this.reconnectCount = reconnectCount;
    return this;
  }

  /**
   * Gets frequency of metrics log print out.
   *
   * @return Frequency of metrics log print out.
   */
  public long getMetricsLogFrequency() {
    return metricsLogFrequency;
  }

  /**
   * Sets frequency of metrics log print out.
   *
   * @param metricsLogFrequency Frequency of metrics log print out.
   * @return reference to this, for fluency
   */
  public IgniteOptions setMetricsLogFrequency(long metricsLogFrequency) {
    this.metricsLogFrequency = metricsLogFrequency;
    return this;
  }

  /**
   * Should return fully configured discovery options. If not provided,
   * TcpDiscovery will be used by default.
   *
   * @return Grid discovery options {@link IgniteDiscoveryOptions}.
   */
  public IgniteDiscoveryOptions getDiscoverySpi() {
    return discoveryOptions;
  }

  /**
   * Sets fully configured instance of {@link IgniteDiscoveryOptions}.
   *
   * @param discoveryOptions {@link IgniteDiscoveryOptions}.
   * @return reference to this, for fluency
   */
  public IgniteOptions setDiscoverySpi(IgniteDiscoveryOptions discoveryOptions) {
    this.discoveryOptions = discoveryOptions;
    return this;
  }

  /**
   * Gets configuration (descriptors) for all caches.
   *
   * @return List of cache configurations.
   */
  public List<IgniteCacheOptions> getCacheConfiguration() {
    return cacheConfiguration;
  }

  /**
   * Sets cache configurations.
   *
   * @param cacheConfiguration Cache configurations.
   * @return reference to this, for fluency
   */
  public IgniteOptions setCacheConfiguration(List<IgniteCacheOptions> cacheConfiguration) {
    this.cacheConfiguration = cacheConfiguration;
    return this;
  }

  public IgniteSslOptions getSslContextFactory() {
    return sslOptions;
  }

  /**
   * Sets SSL options that will be used for creating a secure socket layer.
   *
   * @param sslOptions Ssl options.
   * @return reference to this, for fluency
   */
  public IgniteOptions setSslContextFactory(IgniteSslOptions sslOptions) {
    this.sslOptions = sslOptions;
    return this;
  }

  public boolean isShutdownOnSegmentation() {
    return shutdownOnSegmentation;
  }

  /**
   * Sets that vertx will be shutdown when the cache goes into segmented state.
   * Defaults to true
   *
   * @param shutdownOnSegmentation boolean flag.
   * @return reference to this, for fluency
   */
  public IgniteOptions setShutdownOnSegmentation(boolean shutdownOnSegmentation) {
    this.shutdownOnSegmentation = shutdownOnSegmentation;
    return this;
  }

  /**
   * Get default data region start size.
   * Default to 256 MB
   *
   * @return size in bytes.
   */
  public long getDefaultRegionInitialSize() {
    return defaultRegionInitialSize;
  }

  /**
   * Sets default data region start size.
   *
   * @param defaultRegionInitialSize size in bytes.
   * @return reference to this, for fluency
   */
  public IgniteOptions setDefaultRegionInitialSize(long defaultRegionInitialSize) {
    this.defaultRegionInitialSize = defaultRegionInitialSize;
    return this;
  }

  /**
   * Get default data region maximum size.
   * Default to 20% of physical memory available
   *
   * @return size in bytes.
   */
  public long getDefaultRegionMaxSize() {
    return defaultRegionMaxSize;
  }

  /**
   * Sets default data region maximum size.
   *
   * @param defaultRegionMaxSize size in bytes.
   * @return reference to this, for fluency
   */
  public IgniteOptions setDefaultRegionMaxSize(long defaultRegionMaxSize) {
    this.defaultRegionMaxSize = defaultRegionMaxSize;
    return this;
  }

  /**
   * Convert to JSON
   *
   * @return the JSON
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    IgniteOptionsConverter.toJson(this, json);
    return json;
  }
}
