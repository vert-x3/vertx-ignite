package io.vertx.spi.cluster.ignite;

import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.ignite.configuration.IgniteConfiguration.DFLT_METRICS_LOG_FREQ;
import static org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi.*;
import static org.apache.ignite.ssl.SslContextFactory.*;
import static org.junit.Assert.*;

public class IgniteOptionsTest {

  @Test
  public void defaults() {
    IgniteOptions options = new IgniteOptions();
    assertNull(options.getLocalHost());
    assertEquals(0, options.getIncludeEventTypes().size());
    assertEquals(DFLT_PORT, options.getLocalPort());
    assertEquals(DFLT_CONN_PER_NODE, options.getConnectionsPerNode());
    assertEquals(DFLT_CONN_TIMEOUT, options.getConnectTimeout());
    assertEquals(DFLT_IDLE_CONN_TIMEOUT, options.getIdleConnectionTimeout());
    assertEquals(DFLT_RECONNECT_CNT, options.getReconnectCount());
    assertEquals(DFLT_MAX_CONN_TIMEOUT, options.getMaxConnectTimeout());
    assertEquals(DFLT_METRICS_LOG_FREQ, options.getMetricsLogFrequency());
    assertEquals("TcpDiscoveryMulticastIpFinder", options.getDiscoverySpi().getType());
    assertEquals(0, options.getDiscoverySpi().getProperties().size());
    assertEquals(0, options.getCacheConfiguration().size());
    assertEquals(DFLT_SSL_PROTOCOL, options.getSslContextFactory().getProtocol());
    assertEquals(DFLT_KEY_ALGORITHM, options.getSslContextFactory().getKeyAlgorithm());
    assertEquals(DFLT_STORE_TYPE, options.getSslContextFactory().getKeyStoreType());
    assertEquals(DFLT_STORE_TYPE, options.getSslContextFactory().getTrustStoreType());
    assertNull(options.getSslContextFactory().getKeyStoreFilePath());
    assertNull(options.getSslContextFactory().getTrustStoreFilePath());
    assertFalse(options.getSslContextFactory().isTrustAll());
    assertTrue(options.isShutdownOnSegmentation());
  }

  @Test
  public void fromEmptyJson() {
    IgniteOptions options = new IgniteOptions(new JsonObject());
    assertNull(options.getLocalHost());
    assertEquals(0, options.getIncludeEventTypes().size());
    assertEquals(DFLT_PORT, options.getLocalPort());
    assertEquals(DFLT_CONN_PER_NODE, options.getConnectionsPerNode());
    assertEquals(DFLT_CONN_TIMEOUT, options.getConnectTimeout());
    assertEquals(DFLT_IDLE_CONN_TIMEOUT, options.getIdleConnectionTimeout());
    assertEquals(DFLT_RECONNECT_CNT, options.getReconnectCount());
    assertEquals(DFLT_MAX_CONN_TIMEOUT, options.getMaxConnectTimeout());
    assertEquals(DFLT_METRICS_LOG_FREQ, options.getMetricsLogFrequency());
    assertEquals("TcpDiscoveryMulticastIpFinder", options.getDiscoverySpi().getType());
    assertEquals(0, options.getDiscoverySpi().getProperties().size());
    assertEquals(0, options.getCacheConfiguration().size());
    assertEquals(DFLT_SSL_PROTOCOL, options.getSslContextFactory().getProtocol());
    assertEquals(DFLT_KEY_ALGORITHM, options.getSslContextFactory().getKeyAlgorithm());
    assertEquals(DFLT_STORE_TYPE, options.getSslContextFactory().getKeyStoreType());
    assertEquals(DFLT_STORE_TYPE, options.getSslContextFactory().getTrustStoreType());
    assertNull(options.getSslContextFactory().getKeyStoreFilePath());
    assertNull(options.getSslContextFactory().getTrustStoreFilePath());
    assertFalse(options.getSslContextFactory().isTrustAll());
    assertTrue(options.isShutdownOnSegmentation());
  }

  private void checkConfig(IgniteOptions options, IgniteConfiguration config) {
    assertEquals(options.getLocalHost(), config.getLocalHost());
    assertEquals("TcpCommunicationSpi", config.getCommunicationSpi().getClass().getSimpleName());
    assertEquals(options.getLocalPort(), ((TcpCommunicationSpi) config.getCommunicationSpi()).getLocalPort());
    assertEquals(options.getConnectionsPerNode(), ((TcpCommunicationSpi) config.getCommunicationSpi()).getConnectionsPerNode());
    assertEquals(options.getConnectTimeout(), ((TcpCommunicationSpi) config.getCommunicationSpi()).getConnectTimeout());
    assertEquals(options.getIdleConnectionTimeout(), ((TcpCommunicationSpi) config.getCommunicationSpi()).getIdleConnectionTimeout());
    assertEquals(options.getMaxConnectTimeout(), ((TcpCommunicationSpi) config.getCommunicationSpi()).getMaxConnectTimeout());
    assertEquals(options.getReconnectCount(), ((TcpCommunicationSpi) config.getCommunicationSpi()).getReconnectCount());
    assertEquals(options.getIncludeEventTypes(), Arrays.stream(config.getIncludeEventTypes())
      .mapToObj(IgniteOptions.IgniteEventType::valueOf)
      .map(Objects::toString)
      .collect(Collectors.toList()));
    assertEquals(options.getMetricsLogFrequency(), config.getMetricsLogFrequency());
    assertEquals("TcpDiscoverySpi", config.getDiscoverySpi().getName());
    assertEquals(options.getDiscoverySpi().getProperties().getLong("joinTimeout").longValue(), ((TcpDiscoverySpi) config.getDiscoverySpi()).getJoinTimeout());
    assertEquals(options.getDiscoverySpi().getType(), ((TcpDiscoverySpi) config.getDiscoverySpi()).getIpFinder().getClass().getSimpleName());
    assertEquals(options.getSslContextFactory().getProtocol(), config.getSslContextFactory().create().getProtocol());
    assertEquals(1, config.getCacheConfiguration().length);
    assertEquals(options.getCacheConfiguration().get(0).getName(), config.getCacheConfiguration()[0].getName());
    assertEquals(options.getCacheConfiguration().get(0).getAtomicityMode(), config.getCacheConfiguration()[0].getAtomicityMode().name());
    assertEquals(options.getCacheConfiguration().get(0).getCacheMode(), config.getCacheConfiguration()[0].getCacheMode().name());
    assertEquals(options.getCacheConfiguration().get(0).getGroupName(), config.getCacheConfiguration()[0].getGroupName());
    assertEquals(options.getCacheConfiguration().get(0).getPartitionLossPolicy(), config.getCacheConfiguration()[0].getPartitionLossPolicy().name());
    assertEquals(options.getCacheConfiguration().get(0).getRebalanceMode(), config.getCacheConfiguration()[0].getRebalanceMode().name());
    assertEquals(options.getCacheConfiguration().get(0).getRebalanceDelay(), config.getCacheConfiguration()[0].getRebalanceDelay());
    assertEquals(options.getCacheConfiguration().get(0).getRebalanceOrder(), config.getCacheConfiguration()[0].getRebalanceOrder());
    assertEquals(options.getCacheConfiguration().get(0).getWriteSynchronizationMode(), config.getCacheConfiguration()[0].getWriteSynchronizationMode().name());
    assertEquals(options.getCacheConfiguration().get(0).getBackups(), config.getCacheConfiguration()[0].getBackups());
    assertEquals(options.getCacheConfiguration().get(0).getDefaultLockTimeout(), config.getCacheConfiguration()[0].getDefaultLockTimeout());
    assertEquals(options.getCacheConfiguration().get(0).getMaxConcurrentAsyncOperations(), config.getCacheConfiguration()[0].getMaxConcurrentAsyncOperations());
    assertEquals(options.getCacheConfiguration().get(0).getMaxQueryInteratorsCount(), config.getCacheConfiguration()[0].getMaxQueryIteratorsCount());
    assertEquals(options.getCacheConfiguration().get(0).isEagerTtl(), config.getCacheConfiguration()[0].isEagerTtl());
    assertEquals(options.getCacheConfiguration().get(0).isCopyOnRead(), config.getCacheConfiguration()[0].isCopyOnRead());
    assertEquals(options.getCacheConfiguration().get(0).isEventsDisabled(), config.getCacheConfiguration()[0].isEventsDisabled());
    assertEquals(options.getCacheConfiguration().get(0).isInvalidate(), config.getCacheConfiguration()[0].isInvalidate());
    assertEquals(options.getCacheConfiguration().get(0).isOnheapCacheEnabled(), config.getCacheConfiguration()[0].isOnheapCacheEnabled());
    assertEquals(options.getCacheConfiguration().get(0).isReadFromBackup(), config.getCacheConfiguration()[0].isReadFromBackup());
  }

  private IgniteOptions createIgniteOptions() {
    return new IgniteOptions()
      .setLocalHost("localHost")
      .setLocalPort(12345)
      .setConnectionsPerNode(2)
      .setConnectTimeout(2000L)
      .setIdleConnectionTimeout(300_000L)
      .setMaxConnectTimeout(200_000L)
      .setReconnectCount(20)
      .setIncludeEventTypes(Arrays.asList("EVT_CACHE_OBJECT_PUT", "EVT_CACHE_OBJECT_REMOVED"))
      .setMetricsLogFrequency(10L)
      .setDiscoverySpi(new IgniteDiscoveryOptions()
        .setType("TcpDiscoveryVmIpFinder")
        .setProperties(new JsonObject().put("joinTimeout", 10_000L)))
      .setSslContextFactory(new IgniteSslOptions()
        .setProtocol("TLSv1.2")
        .setKeyAlgorithm("SunX509")
        .setKeyStoreType("JKS")
        .setKeyStoreFilePath("src/test/resources/server.jks")
        .setKeyStorePassword("123456")
        .setTrustStoreType("JKS")
        .setTrustStoreFilePath("src/test/resources/server.jks")
        .setTrustStorePassword("123456")
        .setTrustAll(true))
      .setCacheConfiguration(Collections.singletonList(new IgniteCacheOptions()
        .setName("*")
        .setAtomicityMode("TRANSACTIONAL")
        .setBackups(1)
        .setCacheMode("LOCAL")
        .setCopyOnRead(false)
        .setDefaultLockTimeout(1000L)
        .setEagerTtl(false)
        .setEventsDisabled(true)
        .setGroupName("testGroup")
        .setInvalidate(true)
        .setMaxConcurrentAsyncOperations(100)
        .setMaxQueryInteratorsCount(512)
        .setOnheapCacheEnabled(true)
        .setPartitionLossPolicy("READ_WRITE_ALL")
        .setReadFromBackup(false)
        .setRebalanceDelay(100L)
        .setRebalanceMode("SYNC")
        .setRebalanceOrder(1)
        .setWriteSynchronizationMode("FULL_SYNC")));
  }

  @Test
  public void toConfig() {
    IgniteOptions options = createIgniteOptions();
    IgniteConfiguration config = options.toConfig();
    checkConfig(options, config);
  }

  @Test(expected = VertxException.class)
  public void noDiscoverySpiFound() {
    IgniteOptions options = new IgniteOptions()
      .setDiscoverySpi(new IgniteDiscoveryOptions()
        .setType("NotExistingSpi"));
    options.toConfig();
  }

  @Test(expected = IllegalArgumentException.class)
  public void unsupportedEventType() {
    IgniteOptions options = new IgniteOptions()
      .setIncludeEventTypes(Arrays.asList("EVT_NOT_EXISTING1", "EVT_NOT_EXISTING2"));
    options.toConfig();
  }

  private void checkJson(IgniteOptions options, JsonObject json) {
    assertEquals(options.getLocalHost(), json.getString("localHost"));
    assertEquals(options.getLocalPort(), json.getInteger("localPort").intValue());
    assertEquals(options.getConnectionsPerNode(), json.getInteger("connectionsPerNode").intValue());
    assertEquals(options.getConnectTimeout(), json.getLong("connectTimeout").longValue());
    assertEquals(options.getIdleConnectionTimeout(), json.getLong("idleConnectionTimeout").longValue());
    assertEquals(options.getMaxConnectTimeout(), json.getLong("maxConnectTimeout").longValue());
    assertEquals(options.getReconnectCount(), json.getInteger("reconnectCount").intValue());
    assertEquals(options.getIncludeEventTypes(), json.getJsonArray("includeEventTypes").getList());
    assertEquals(options.getMetricsLogFrequency(), json.getLong("metricsLogFrequency").longValue());
    assertEquals(options.isShutdownOnSegmentation(), json.getBoolean("shutdownOnSegmentation"));
    assertEquals(options.getDiscoverySpi().getType(), json.getJsonObject("discoverySpi").getString("type"));
    assertEquals(options.getDiscoverySpi().getProperties().getLong("joinTimeout"), json.getJsonObject("discoverySpi").getJsonObject("properties").getLong("joinTimeout"));
    assertEquals(options.getSslContextFactory().getProtocol(), json.getJsonObject("sslContextFactory").getString("protocol"));
    assertEquals(options.getSslContextFactory().getKeyAlgorithm(), json.getJsonObject("sslContextFactory").getString("keyAlgorithm"));
    assertEquals(options.getSslContextFactory().getKeyStoreType(), json.getJsonObject("sslContextFactory").getString("keyStoreType"));
    assertEquals(options.getSslContextFactory().getKeyStoreFilePath(), json.getJsonObject("sslContextFactory").getString("keyStoreFilePath"));
    assertEquals(options.getSslContextFactory().getKeyStorePassword(), json.getJsonObject("sslContextFactory").getString("keyStorePassword"));
    assertEquals(options.getSslContextFactory().getTrustStoreType(), json.getJsonObject("sslContextFactory").getString("trustStoreType"));
    assertEquals(options.getSslContextFactory().getTrustStoreFilePath(), json.getJsonObject("sslContextFactory").getString("trustStoreFilePath"));
    assertEquals(options.getSslContextFactory().getTrustStorePassword(), json.getJsonObject("sslContextFactory").getString("trustStorePassword"));
    assertEquals(options.getSslContextFactory().isTrustAll(), json.getJsonObject("sslContextFactory").getBoolean("trustAll"));
    assertEquals(1, json.getJsonArray("cacheConfiguration").size());
    assertEquals(options.getCacheConfiguration().get(0).getName(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getString("name"));
    assertEquals(options.getCacheConfiguration().get(0).getAtomicityMode(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getString("atomicityMode"));
    assertEquals(options.getCacheConfiguration().get(0).getCacheMode(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getString("cacheMode"));
    assertEquals(options.getCacheConfiguration().get(0).getGroupName(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getString("groupName"));
    assertEquals(options.getCacheConfiguration().get(0).getPartitionLossPolicy(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getString("partitionLossPolicy"));
    assertEquals(options.getCacheConfiguration().get(0).getRebalanceMode(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getString("rebalanceMode"));
    assertEquals(options.getCacheConfiguration().get(0).getRebalanceDelay(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getLong("rebalanceDelay").longValue());
    assertEquals(options.getCacheConfiguration().get(0).getRebalanceOrder(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getInteger("rebalanceOrder").intValue());
    assertEquals(options.getCacheConfiguration().get(0).getWriteSynchronizationMode(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getString("writeSynchronizationMode"));
    assertEquals(options.getCacheConfiguration().get(0).getBackups(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getInteger("backups").intValue());
    assertEquals(options.getCacheConfiguration().get(0).getDefaultLockTimeout(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getLong("defaultLockTimeout").longValue());
    assertEquals(options.getCacheConfiguration().get(0).getMaxConcurrentAsyncOperations(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getInteger("maxConcurrentAsyncOperations").intValue());
    assertEquals(options.getCacheConfiguration().get(0).getMaxQueryInteratorsCount(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getInteger("maxQueryInteratorsCount").intValue());
    assertEquals(options.getCacheConfiguration().get(0).isEagerTtl(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getBoolean("eagerTtl"));
    assertEquals(options.getCacheConfiguration().get(0).isCopyOnRead(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getBoolean("copyOnRead"));
    assertEquals(options.getCacheConfiguration().get(0).isEventsDisabled(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getBoolean("eventsDisabled"));
    assertEquals(options.getCacheConfiguration().get(0).isInvalidate(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getBoolean("invalidate"));
    assertEquals(options.getCacheConfiguration().get(0).isOnheapCacheEnabled(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getBoolean("onheapCacheEnabled"));
    assertEquals(options.getCacheConfiguration().get(0).isReadFromBackup(), json.getJsonArray("cacheConfiguration").getJsonObject(0).getBoolean("readFromBackup"));
  }

  @Test
  public void toJson() {
    IgniteOptions options = createIgniteOptions();
    JsonObject json = options.toJson();
    checkJson(options, json);
  }

  private static final String IGNITE_JSON = "{\n" +
    "  \"connectTimeout\": 2000,\n" +
    "  \"connectionsPerNode\": 2,\n" +
    "  \"idleConnectionTimeout\": 300000,\n" +
    "  \"includeEventTypes\": [\"EVT_CACHE_OBJECT_PUT\", \"EVT_CACHE_OBJECT_REMOVED\"],\n" +
    "  \"localHost\": \"localHost\",\n" +
    "  \"localPort\": 12345,\n" +
    "  \"maxConnectTimeout\": 200000,\n" +
    "  \"metricsLogFrequency\": 10,\n" +
    "  \"reconnectCount\": 20,\n" +
    "  \"shutdownOnSegmentation\": true,\n" +
    "  \"discoverySpi\": {\n" +
    "    \"type\": \"TcpDiscoveryVmIpFinder\",\n" +
    "    \"properties\": {\n" +
    "      \"joinTimeout\": 10000\n" +
    "    }\n" +
    "  },\n" +
    "  \"cacheConfiguration\": [{\n" +
    "    \"name\": \"*\",\n" +
    "    \"atomicityMode\": \"TRANSACTIONAL\",\n" +
    "    \"backups\": 1,\n" +
    "    \"cacheMode\": \"LOCAL\",\n" +
    "    \"copyOnRead\": false,\n" +
    "    \"defaultLockTimeout\": 1000,\n" +
    "    \"eagerTtl\": false,\n" +
    "    \"encryptionEnabled\": false,\n" +
    "    \"eventsDisabled\": true,\n" +
    "    \"groupName\": \"testGroup\",\n" +
    "    \"invalidate\": true,\n" +
    "    \"maxConcurrentAsyncOperations\": 100,\n" +
    "    \"maxQueryInteratorsCount\": 512,\n" +
    "    \"onheapCacheEnabled\": true,\n" +
    "    \"partitionLossPolicy\": \"READ_WRITE_ALL\",\n" +
    "    \"readFromBackup\": false,\n" +
    "    \"rebalanceDelay\": 100,\n" +
    "    \"rebalanceMode\": \"SYNC\",\n" +
    "    \"rebalanceOrder\": 1,\n" +
    "    \"writeSynchronizationMode\": \"FULL_SYNC\"\n" +
    "  }],\n" +
    "  \"sslContextFactory\": {\n" +
    "    \"keyAlgorithm\": \"SunX509\",\n" +
    "    \"keyStoreFilePath\": \"src/test/resources/server.jks\",\n" +
    "    \"keyStorePassword\": \"123456\",\n" +
    "    \"keyStoreType\": \"JKS\",\n" +
    "    \"protocol\": \"TLSv1.2\",\n" +
    "    \"trustAll\": true,\n" +
    "    \"trustStoreFilePath\": \"src/test/resources/server.jks\",\n" +
    "    \"trustStorePassword\": \"123456\",\n" +
    "    \"trustStoreType\": \"JKS\"\n" +
    "  }\n" +
    "}";

  @Test
  public void fromJson() {
    JsonObject json = new JsonObject(IGNITE_JSON);
    IgniteOptions options = new IgniteOptions(json);
    checkJson(options, json);
  }

  @Test
  public void copy() {
    IgniteOptions options = createIgniteOptions();
    IgniteOptions copy = new IgniteOptions(options);
    assertEquals(options.getLocalHost(), copy.getLocalHost());
  }
}
