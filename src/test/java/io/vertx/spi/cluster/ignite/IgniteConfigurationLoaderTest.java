package io.vertx.spi.cluster.ignite;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.AffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.spi.discovery.DiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.vertx.spi.cluster.ignite.cfg.IgniteConfigurationLoader;
import io.vertx.spi.cluster.ignite.impl.IgniteConfigurationLoaderImpl;

public class IgniteConfigurationLoaderTest {

  IgniteConfigurationLoader loader;
  
  @Before
  public void initLoader() {
    this.loader = new IgniteConfigurationLoaderImpl();
  }
  
  @Test
  public void testDefaultConfiguration() throws Exception {
    URL resource = getClass().getResource("/default-ignite.json");
    String json = new String(Files.readAllBytes(Paths.get(resource.toURI())), "UTF-8");
    IgniteConfiguration cfg = this.loader.fromJson(json);
    Assert.assertEquals(0, cfg.getMetricsLogFrequency());
    Assert.assertArrayEquals(new int[]{EventType.EVT_CACHE_OBJECT_REMOVED}, cfg.getIncludeEventTypes());
    
    DiscoverySpi discoverySpi = cfg.getDiscoverySpi();
    Assert.assertNotNull(discoverySpi);
    Assert.assertTrue(discoverySpi instanceof TcpDiscoverySpi);
    TcpDiscoverySpi tcpDiscoverySpi = (TcpDiscoverySpi) discoverySpi;
    TcpDiscoveryIpFinder ipFinder = tcpDiscoverySpi.getIpFinder();
    Assert.assertNotNull(ipFinder);
    Assert.assertTrue(ipFinder instanceof TcpDiscoveryMulticastIpFinder);
    
    CacheConfiguration[] cacheConfigurations = cfg.getCacheConfiguration();
    Assert.assertNotNull(cacheConfigurations);
    Assert.assertTrue(cacheConfigurations.length==1);
    CacheConfiguration cacheConfiguration = cacheConfigurations[0];
    Assert.assertEquals("*", cacheConfiguration.getName());
    Assert.assertEquals(CacheMode.PARTITIONED, cacheConfiguration.getCacheMode());
    Assert.assertEquals(1, cacheConfiguration.getBackups());
    Assert.assertEquals(false, cacheConfiguration.isReadFromBackup());
  }
 
  @Test
  public void testTestingConfiguration() throws Exception {
    URL resource = getClass().getResource("/ignite.json");
    String json = new String(Files.readAllBytes(Paths.get(resource.toURI())), "UTF-8");
    IgniteConfiguration cfg = this.loader.fromJson(json);
    Assert.assertEquals(0, cfg.getMetricsLogFrequency());
    Assert.assertArrayEquals(new int[]{EventType.EVT_CACHE_OBJECT_REMOVED}, cfg.getIncludeEventTypes());
    
    DiscoverySpi discoverySpi = cfg.getDiscoverySpi();
    Assert.assertNotNull(discoverySpi);
    Assert.assertTrue(discoverySpi instanceof TcpDiscoverySpi);
    TcpDiscoverySpi tcpDiscoverySpi = (TcpDiscoverySpi) discoverySpi;
    TcpDiscoveryIpFinder ipFinder = tcpDiscoverySpi.getIpFinder();
    Assert.assertNotNull(ipFinder);
    Assert.assertTrue(ipFinder instanceof TcpDiscoveryVmIpFinder);
    TcpDiscoveryVmIpFinder tcpDiscoveryVmIpFinder = (TcpDiscoveryVmIpFinder) ipFinder;
    Collection<InetSocketAddress> registeredAddresses = tcpDiscoveryVmIpFinder.getRegisteredAddresses();
    Assert.assertEquals(50, registeredAddresses.size());
    InetSocketAddress address = registeredAddresses.iterator().next();
    Assert.assertEquals("127.0.0.1", address.getAddress().getHostAddress());
    
    CacheConfiguration[] cacheConfigurations = cfg.getCacheConfiguration();
    Assert.assertNotNull(cacheConfigurations);
    Assert.assertTrue(cacheConfigurations.length==1);
    CacheConfiguration cacheConfiguration = cacheConfigurations[0];
    Assert.assertEquals("*", cacheConfiguration.getName());
    Assert.assertEquals(CacheMode.PARTITIONED, cacheConfiguration.getCacheMode());
    Assert.assertEquals(1, cacheConfiguration.getBackups());
    Assert.assertEquals(false, cacheConfiguration.isReadFromBackup());
    AffinityFunction affinity = cacheConfiguration.getAffinity(); 
    Assert.assertNotNull(affinity);
    Assert.assertEquals(128, affinity.partitions());
  }
  
}
