/*
 * Copyright (c) 2015 The original author or authors
 * ---------------------------------
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.spi.cluster.ignite;

import io.vertx.core.*;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.spi.cluster.*;
import io.vertx.spi.cluster.ignite.impl.*;
import io.vertx.spi.cluster.ignite.util.ConfigHelper;
import org.apache.ignite.*;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.failure.StopNodeFailureHandler;
import org.apache.ignite.internal.processors.cache.IgniteCacheProxy;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.lifecycle.LifecycleEventType;
import org.apache.ignite.plugin.segmentation.SegmentationPolicy;

import javax.cache.CacheException;
import javax.cache.configuration.Factory;
import javax.cache.expiry.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static javax.cache.expiry.Duration.ETERNAL;
import static org.apache.ignite.events.EventType.*;
import static org.apache.ignite.internal.IgniteComponentType.SPRING;

/**
 * Apache Ignite based cluster manager.
 *
 * @author Andrey Gura
 * @author Lukas Prettenthaler
 */
public class IgniteClusterManager implements ClusterManager {

  private static final Logger log = LoggerFactory.getLogger(IgniteClusterManager.class);

  // Default Ignite configuration file
  private static final String DEFAULT_CONFIG_FILE = "default-ignite.json";
  // User defined Ignite configuration file
  private static final String CONFIG_FILE = "ignite.json";
  // Fallback XML Ignite configuration file (requires ignite-spring dependency)
  private static final String XML_CONFIG_FILE = "ignite.xml";

  private static final String VERTX_NODE_PREFIX = "vertx.ignite.node.";

  private static final String LOCK_SEMAPHORE_PREFIX = "__vertx.";

  private static final Factory<ExpiryPolicy> DEFAULT_EXPIRY_POLICY_FACTORY = ModifiedExpiryPolicy.factoryOf(ETERNAL);

  private static final int[] IGNITE_EVENTS = new int[]{EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED, EVT_NODE_SEGMENTED};

  private VertxInternal vertx;
  private RegistrationListener registrationListener;

  private IgniteConfiguration extCfg;
  private IgniteOptions extOptions;
  private URL extConfigUrl;

  private Ignite ignite;
  private boolean customIgnite;
  private boolean shutdownOnSegmentation;
  private boolean shutdownOnNodeStop;
  private long delayAfterStart;

  private String nodeId;
  private NodeInfo nodeInfo;
  private IgniteCache<String, IgniteNodeInfo> nodeInfoMap;
  private SubsMapHelper subsMapHelper;
  private IgnitePredicate<Event> eventListener;

  private volatile boolean active;
  private volatile NodeListener nodeListener;

  private final Object monitor = new Object();

  private ExecutorService lockReleaseExec;

  /**
   * Default constructor. Cluster manager will get configuration from classpath.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager() {
    setIgniteProperties();
  }

  /**
   * Creates cluster manager instance with given Ignite configuration.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param extCfg {@code IgniteConfiguration} instance.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(IgniteConfiguration extCfg) {
    setIgniteProperties();
    this.extCfg = extCfg;
  }

  /**
   * Creates cluster manager instance with given Spring XML configuration file.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param configFile {@code URL} path to Spring XML configuration file.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(URL configFile) {
    setIgniteProperties();
    this.extConfigUrl = configFile;
  }

  /**
   * Creates cluster manager instance with given JSON configuration.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param jsonConfig {@code JsonObject} JSON configuration.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(JsonObject jsonConfig) {
    this(new IgniteOptions(jsonConfig));
  }

  /**
   * Creates cluster manager instance with given IgniteOptions.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param extOptions {@code IgniteOptions} options object.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(IgniteOptions extOptions) {
    setIgniteProperties();
    this.extOptions = extOptions;
  }

  /**
   * Creates cluster manager instance with given {@code Ignite} instance.
   *
   * @param ignite {@code Ignite} instance.
   */
  public IgniteClusterManager(Ignite ignite) {
    Objects.requireNonNull(ignite, "Ignite instance can't be null.");
    this.ignite = ignite;
    this.customIgnite = true;
  }

  /**
   * Returns instance of {@code Ignite}.
   *
   * @return {@code Ignite} instance.
   */
  public Ignite getIgniteInstance() {
    return ignite;
  }

  @Override
  public void init(Vertx vertx) {
    this.vertx = (VertxInternal) vertx;
  }

  @Override
  public void registrationListener(RegistrationListener registrationListener) {
    this.registrationListener = registrationListener;
  }

  @Override
  public void nodeListener(NodeListener nodeListener) {
    this.nodeListener = nodeListener;
  }

  @Override
  public <K, V> void getAsyncMap(String name, Completable<AsyncMap<K, V>> promise) {
    vertx.<AsyncMap<K, V>>executeBlocking(() -> new AsyncMapImpl<>(getCache(name), vertx)).onComplete(promise);
  }

  @Override
  public <K, V> Map<K, V> getSyncMap(String name) {
    return new MapImpl<>(getCache(name));
  }

  @Override
  public void getLockWithTimeout(String name, long timeout, Completable<Lock> promise) {
    vertx.<Lock>executeBlocking(() -> {
      IgniteSemaphore semaphore = ignite.semaphore(LOCK_SEMAPHORE_PREFIX + name, 1, true, true);
      boolean locked;
      long remaining = timeout;
      do {
        long start = System.nanoTime();
        locked = semaphore.tryAcquire(remaining, TimeUnit.MILLISECONDS);
        remaining = remaining - TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS);
      } while (!locked && remaining > 0);
      if (locked) {
        return new LockImpl(semaphore, lockReleaseExec);
      } else {
        throw new VertxException("Timed out waiting to get lock " + name, true);
      }
    }, false).onComplete(promise);
  }

  @Override
  public void getCounter(String name, Completable<Counter> promise) {
    vertx.<Counter>executeBlocking(() -> new CounterImpl(ignite.atomicLong(name, 0, true))).onComplete(promise);
  }

  @Override
  public String getNodeId() {
    return nodeId;
  }

  @Override
  public void setNodeInfo(NodeInfo nodeInfo, Completable<Void> promise) {
    synchronized (this) {
      this.nodeInfo = nodeInfo;
    }
    IgniteNodeInfo value = new IgniteNodeInfo(nodeInfo);
    vertx.<Void>executeBlocking(() -> {
      nodeInfoMap.put(nodeId, value);
      return null;
    }, false).onComplete(promise);
  }

  @Override
  public synchronized NodeInfo getNodeInfo() {
    return nodeInfo;
  }

  @Override
  public void getNodeInfo(String id, Completable<NodeInfo> promise) {
    nodeInfoMap.getAsync(id).listen(fut -> {
      try {
        IgniteNodeInfo value = fut.get();
        if (value != null) {
          promise.succeed(value.unwrap());
        } else {
          promise.fail("Not a member of the cluster");
        }
      } catch (IgniteException e) {
        promise.fail(e);
      }
    });
  }

  @Override
  public List<String> getNodes() {
    try {
      Collection<ClusterNode> nodes = ignite.cluster().nodes();
      List<String> nodeIds = new ArrayList<>(nodes.size());
      for (ClusterNode node : nodes) {
        nodeIds.add(nodeId(node));
      }
      return nodeIds;
    } catch (IllegalStateException e) {
      log.debug(e.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public void join(Completable<Void> promise) {
    vertx.<Void>executeBlocking(() -> {
      synchronized (monitor) {
        if (!active) {
          active = true;

          lockReleaseExec = Executors.newCachedThreadPool(r -> new Thread(r, "vertx-ignite-service-release-lock-thread"));

          if (!customIgnite) {
            IgniteConfiguration cfg = prepareConfig();
            cfg.setLifecycleBeans(e -> {
              if (e == LifecycleEventType.AFTER_NODE_STOP && shutdownOnNodeStop && active) {
                vertx.close();
              }
            });
            ignite = Ignition.start(cfg);
          }
          nodeId = nodeId(ignite.cluster().localNode());

          eventListener = event -> {
            if (!isActive()) {
              return false;
            }

            vertx.<Void>executeBlocking(() -> {
              String id = nodeId(((DiscoveryEvent) event).eventNode());
              switch (event.type()) {
                case EVT_NODE_JOINED:
                  notifyNodeListener(listener -> listener.nodeAdded(id));
                  log.debug("node " + id + " joined the cluster");
                  return null;
                case EVT_NODE_LEFT:
                case EVT_NODE_FAILED:
                  if (cleanNodeInfos(id)) {
                    cleanSubs(id);
                  }
                  notifyNodeListener(listener -> listener.nodeLeft(id));
                  log.debug("node " + id + " left the cluster");
                  return null;
                case EVT_NODE_SEGMENTED:
                  if (customIgnite || !shutdownOnSegmentation) {
                    log.warn("node got segmented");
                  } else {
                    log.warn("node got segmented and will be shut down");
                    vertx.close();
                  }
                  throw new IllegalStateException("node is stopped");
                default:
                  throw new IllegalStateException("event not known");
              }
            });

            return true;
          };

          ignite.events().localListen(eventListener, IGNITE_EVENTS);
          subsMapHelper = new SubsMapHelper(ignite, registrationListener, vertx);
          nodeInfoMap = ignite.getOrCreateCache("__vertx.nodeInfo");

          MILLISECONDS.sleep(delayAfterStart);
        }
        return null;
      }
    }).onComplete(promise);
  }

  @Override
  public void leave(Completable<Void> promise) {
    vertx.<Void>executeBlocking(() -> {
      synchronized (monitor) {
        if (active) {
          active = false;
          lockReleaseExec.shutdown();
          try {
            if (eventListener != null) {
              ignite.events().stopLocalListen(eventListener, IGNITE_EVENTS);
            }
            subsMapHelper.leave();
            if (!customIgnite) {
              ignite.close();
            }
          } catch (Exception e) {
            log.error(e);
          }
          subsMapHelper = null;
          nodeInfoMap = null;
        }
      }

      return null;
    }).onComplete(promise);
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void addRegistration(String address, RegistrationInfo registrationInfo, Completable<Void> promise) {
    vertx.executeBlocking(() -> subsMapHelper.put(address, registrationInfo), false).onComplete(promise);
  }

  @Override
  public void removeRegistration(String address, RegistrationInfo registrationInfo, Completable<Void> promise) {
    vertx.<Void>executeBlocking(() -> subsMapHelper.remove(address, registrationInfo), false).onComplete(promise);
  }

  @Override
  public void getRegistrations(String address, Completable<List<RegistrationInfo>> promise) {
    vertx.executeBlocking(() -> subsMapHelper.get(address), false).onComplete(promise);
  }

  private void cleanSubs(String id) {
    subsMapHelper.removeAllForNode(id);
  }

  private boolean cleanNodeInfos(String nid) {
    try {
      return nodeInfoMap.remove(nid);
    } catch (IllegalStateException | CacheException e) {
      log.warn("Could not remove nodeInfo (" + nid + "): " + e.getMessage());
    }
    return false;
  }

  private IgniteConfiguration prepareConfig() {
    IgniteConfiguration cfg = null;
    if (extCfg != null) {
      cfg = extCfg;
    } else {
      if (SPRING.inClassPath()) {
        try {
          cfg = ConfigHelper.lookupXmlConfiguration(this.getClass(), XML_CONFIG_FILE);
        } catch (VertxException e) {
          log.debug("xml config could not be loaded");
        }
      }
    }
    if (extConfigUrl != null) {
      cfg = ConfigHelper.loadConfiguration(extConfigUrl);
    }
    if (cfg == null) {
      IgniteOptions options;
      if (extOptions == null) {
        options = new IgniteOptions(ConfigHelper.lookupJsonConfiguration(this.getClass(), CONFIG_FILE, DEFAULT_CONFIG_FILE));
      } else {
        options = extOptions;
      }
      shutdownOnSegmentation = options.isShutdownOnSegmentation();
      shutdownOnNodeStop = options.isShutdownOnNodeStop();
      delayAfterStart = options.getDelayAfterStart();
      cfg = ConfigHelper.toIgniteConfig(vertx, options)
        .setGridLogger(new VertxLogger());
    }

    UUID uuid = UUID.randomUUID();
    cfg.setNodeId(uuid);
    cfg.setIgniteInstanceName(VERTX_NODE_PREFIX + uuid);
    cfg.setSegmentationPolicy(SegmentationPolicy.NOOP);
    cfg.setFailureHandler(new StopNodeFailureHandler());
    cfg.setAsyncContinuationExecutor(Runnable::run);
    return cfg;
  }

  private <K, V> IgniteCache<K, V> getCache(String name) {
    IgniteCache<K, V> cache = ignite.getOrCreateCache(name);
    if (((IgniteCacheProxy) cache).context().expiry() == null) {
      return cache.withExpiryPolicy(DEFAULT_EXPIRY_POLICY_FACTORY.create());
    }
    return cache;
  }

  private void notifyNodeListener(final Consumer<NodeListener> notify) {
    if (null == notify) return;

    final NodeListener listener = nodeListener;
    if (null == listener) return;

    try {
      notify.accept(listener);
    } catch (final RuntimeException ignore) {
      // exception barrier
    }
  }

  private static String nodeId(ClusterNode node) {
    return node.id().toString();
  }

  private static void setIgniteProperties() {
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
    System.setProperty("IGNITE_UPDATE_NOTIFIER", "false");
  }

  private static class LockImpl implements Lock {
    private final IgniteSemaphore semaphore;
    private final Executor lockReleaseExec;
    private final AtomicBoolean released = new AtomicBoolean();

    private LockImpl(IgniteSemaphore semaphore, Executor lockReleaseExec) {
      this.semaphore = semaphore;
      this.lockReleaseExec = lockReleaseExec;
    }

    @Override
    public void release() {
      if (released.compareAndSet(false, true)) {
        lockReleaseExec.execute(semaphore::release);
      }
    }
  }

  private class CounterImpl implements Counter {
    private final IgniteAtomicLong cnt;

    private CounterImpl(IgniteAtomicLong cnt) {
      this.cnt = cnt;
    }

    @Override
    public Future<Long> get() {
      return vertx.executeBlocking(cnt::get);
    }

    @Override
    public Future<Long> incrementAndGet() {
      return vertx.executeBlocking(cnt::incrementAndGet);
    }

    @Override
    public Future<Long> getAndIncrement() {
      return vertx.executeBlocking(cnt::getAndIncrement);
    }

    @Override
    public Future<Long> decrementAndGet() {
      return vertx.executeBlocking(cnt::decrementAndGet);
    }

    @Override
    public Future<Long> addAndGet(long value) {
      return vertx.executeBlocking(() -> cnt.addAndGet(value));
    }

    @Override
    public Future<Long> getAndAdd(long value) {
      return vertx.executeBlocking(() -> cnt.getAndAdd(value));
    }

    @Override
    public Future<Boolean> compareAndSet(long expected, long value) {
      return vertx.executeBlocking(() -> cnt.compareAndSet(expected, value));
    }
  }
}
