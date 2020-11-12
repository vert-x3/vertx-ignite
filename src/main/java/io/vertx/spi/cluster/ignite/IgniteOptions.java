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
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;

import java.util.*;

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
  private List<String> includeEventTypes;
  private long metricsLogFrequency;
  private IgniteDiscoveryOptions discoveryOptions;
  private List<IgniteCacheOptions> cacheConfiguration;
  private IgniteSslOptions sslOptions;
  private boolean shutdownOnSegmentation;

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
    includeEventTypes = new ArrayList<>();
    metricsLogFrequency = DFLT_METRICS_LOG_FREQ;
    discoveryOptions = new IgniteDiscoveryOptions();
    cacheConfiguration = new ArrayList<>();
    sslOptions = new IgniteSslOptions();
    shutdownOnSegmentation = true;
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
    this.includeEventTypes = options.includeEventTypes;
    this.metricsLogFrequency = options.metricsLogFrequency;
    this.discoveryOptions = options.discoveryOptions;
    this.cacheConfiguration = options.cacheConfiguration;
    this.sslOptions = options.sslOptions;
    this.shutdownOnSegmentation = options.shutdownOnSegmentation;
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
   * Gets array of event types, which will be recorded.
   *
   * @return Include event types.
   */
  public List<String> getIncludeEventTypes() {
    return includeEventTypes;
  }

  /**
   * Sets array of event types, which will be recorded by {@link IgniteClusterManager#join(Promise)}.
   * Note, that either the include event types or the exclude event types can be established.
   *
   * @param includeEventTypes Include event types.
   * @return reference to this, for fluency
   */
  public IgniteOptions setIncludeEventTypes(List<String> includeEventTypes) {
    this.includeEventTypes = includeEventTypes;
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
   * Convert to JSON
   *
   * @return the JSON
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    IgniteOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * Convert to IgniteConfiguration
   *
   * @return the IgniteConfiguration
   */
  public IgniteConfiguration toConfig() {
    IgniteConfiguration configuration = new IgniteConfiguration();
    configuration.setLocalHost(localHost);
    configuration.setCommunicationSpi(new TcpCommunicationSpi()
      .setLocalPort(localPort)
      .setConnectionsPerNode(connectionsPerNode)
      .setConnectTimeout(connectTimeout)
      .setIdleConnectionTimeout(idleConnectionTimeout)
      .setMaxConnectTimeout(maxConnectTimeout)
      .setReconnectCount(reconnectCount));
    configuration.setIncludeEventTypes(includeEventTypes.stream()
      .map(IgniteEventType::valueOf)
      .mapToInt(e -> e.eventType)
      .toArray());
    configuration.setMetricsLogFrequency(metricsLogFrequency);
    configuration.setDiscoverySpi(discoveryOptions.toConfig());
    configuration.setCacheConfiguration(cacheConfiguration.stream()
      .map(IgniteCacheOptions::toConfig)
      .toArray(CacheConfiguration[]::new));
    configuration.setSslContextFactory(sslOptions.toConfig());
    return configuration;
  }

  enum IgniteEventType {
    EVT_CACHE_ENTRY_CREATED(EventType.EVT_CACHE_ENTRY_CREATED),
    EVT_CACHE_ENTRY_DESTROYED(EventType.EVT_CACHE_ENTRY_DESTROYED),
    EVT_CACHE_ENTRY_EVICTED(EventType.EVT_CACHE_ENTRY_EVICTED),
    EVT_CACHE_OBJECT_PUT(EventType.EVT_CACHE_OBJECT_PUT),
    EVT_CACHE_OBJECT_READ(EventType.EVT_CACHE_OBJECT_READ),
    EVT_CACHE_OBJECT_REMOVED(EventType.EVT_CACHE_OBJECT_REMOVED),
    EVT_CACHE_OBJECT_LOCKED(EventType.EVT_CACHE_OBJECT_LOCKED),
    EVT_CACHE_OBJECT_UNLOCKED(EventType.EVT_CACHE_OBJECT_UNLOCKED),
    EVT_CACHE_OBJECT_EXPIRED(EventType.EVT_CACHE_OBJECT_EXPIRED);

    private final int eventType;
    private static final Map<Integer, IgniteEventType> MAP = new HashMap<>();

    static {
      for (IgniteEventType t : IgniteEventType.values()) {
        MAP.put(t.eventType, t);
      }
    }

    IgniteEventType(int eventType) {
      this.eventType = eventType;
    }

    public static IgniteEventType valueOf(int eventType) {
      return MAP.get(eventType);
    }
  }
}
