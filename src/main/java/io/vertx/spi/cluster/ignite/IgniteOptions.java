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
  }

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
  }

  public IgniteOptions(JsonObject options) {
    this();
    IgniteOptionsConverter.fromJson(options, this);
  }

  public String getLocalHost() {
    return localHost;
  }

  public IgniteOptions setLocalHost(String localHost) {
    this.localHost = localHost;
    return this;
  }

  public int getLocalPort() {
    return localPort;
  }

  public IgniteOptions setLocalPort(int localPort) {
    this.localPort = localPort;
    return this;
  }

  public int getConnectionsPerNode() {
    return connectionsPerNode;
  }

  public IgniteOptions setConnectionsPerNode(int connectionsPerNode) {
    this.connectionsPerNode = connectionsPerNode;
    return this;
  }

  public long getConnectTimeout() {
    return connectTimeout;
  }

  public IgniteOptions setConnectTimeout(long connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public long getIdleConnectionTimeout() {
    return idleConnectionTimeout;
  }

  public IgniteOptions setIdleConnectionTimeout(long idleConnectionTimeout) {
    this.idleConnectionTimeout = idleConnectionTimeout;
    return this;
  }

  public long getMaxConnectTimeout() {
    return maxConnectTimeout;
  }

  public IgniteOptions setMaxConnectTimeout(long maxConnectTimeout) {
    this.maxConnectTimeout = maxConnectTimeout;
    return this;
  }

  public int getReconnectCount() {
    return reconnectCount;
  }

  public IgniteOptions setReconnectCount(int reconnectCount) {
    this.reconnectCount = reconnectCount;
    return this;
  }

  public List<String> getIncludeEventTypes() {
    return includeEventTypes;
  }

  public IgniteOptions setIncludeEventTypes(List<String> includeEventTypes) {
    this.includeEventTypes = includeEventTypes;
    return this;
  }

  public long getMetricsLogFrequency() {
    return metricsLogFrequency;
  }

  public IgniteOptions setMetricsLogFrequency(long metricsLogFrequency) {
    this.metricsLogFrequency = metricsLogFrequency;
    return this;
  }

  public IgniteDiscoveryOptions getDiscoverySpi() {
    return discoveryOptions;
  }

  public IgniteOptions setDiscoverySpi(IgniteDiscoveryOptions discoveryOptions) {
    this.discoveryOptions = discoveryOptions;
    return this;
  }

  public List<IgniteCacheOptions> getCacheConfiguration() {
    return cacheConfiguration;
  }

  public IgniteOptions setCacheConfiguration(List<IgniteCacheOptions> cacheConfiguration) {
    this.cacheConfiguration = cacheConfiguration;
    return this;
  }

  public IgniteSslOptions getSslContextFactory() {
    return sslOptions;
  }

  public IgniteOptions setSslContextFactory(IgniteSslOptions sslOptions) {
    this.sslOptions = sslOptions;
    return this;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    IgniteOptionsConverter.toJson(this, json);
    return json;
  }

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
