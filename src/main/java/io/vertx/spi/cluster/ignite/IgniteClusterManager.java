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
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
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
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.plugin.segmentation.SegmentationPolicy;

import javax.cache.CacheException;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

  // Workaround for https://github.com/vert-x3/vertx-ignite/issues/63
  private static final ExpiryPolicy DEFAULT_EXPIRY_POLICY = new ClearExpiryPolicy();

  private static final int[] IGNITE_EVENTS = new int[]{EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED, EVT_NODE_SEGMENTED};

  private VertxInternal vertx;
  private NodeSelector nodeSelector;

  private IgniteConfiguration cfg;
  private Ignite ignite;
  private boolean customIgnite;
  private boolean shutdownOnSegmentation;

  private String nodeId;
  private NodeInfo nodeInfo;
  private IgniteCache<String, IgniteNodeInfo> nodeInfoMap;
  private SubsMapHelper subsMapHelper;
  private NodeListener nodeListener;
  private IgnitePredicate<Event> eventListener;

  private volatile boolean active;

  private final Object monitor = new Object();

  private ExecutorService lockReleaseExec;

  /**
   * Default constructor. Cluster manager will get configuration from classpath.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager() {
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
    if (SPRING.inClassPath()) {
      try {
        cfg = ConfigHelper.lookupXmlConfiguration(this.getClass(), XML_CONFIG_FILE);
      } catch (VertxException e) {
        log.debug("xml config could not be loaded");
      }
    }
    if (cfg == null) {
      IgniteOptions options = new IgniteOptions(ConfigHelper.lookupJsonConfiguration(this.getClass(), CONFIG_FILE, DEFAULT_CONFIG_FILE));
      shutdownOnSegmentation = options.isShutdownOnSegmentation();
      cfg = options.toConfig()
        .setGridLogger(new VertxLogger());
    }
    setNodeId(cfg);
  }

  /**
   * Creates cluster manager instance with given Ignite configuration.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param cfg {@code IgniteConfiguration} instance.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(IgniteConfiguration cfg) {
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
    this.cfg = cfg;
    setNodeId(cfg);
  }

  /**
   * Creates cluster manager instance with given Spring XML configuration file.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param configFile {@code URL} path to Spring XML configuration file.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(URL configFile) {
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
    this.cfg = ConfigHelper.loadConfiguration(configFile);
    setNodeId(cfg);
  }

  /**
   * Creates cluster manager instance with given JSON configuration.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param jsonConfig {@code JsonObject} JSON configuration.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(JsonObject jsonConfig) {
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
    IgniteOptions options = new IgniteOptions(jsonConfig);
    this.shutdownOnSegmentation = options.isShutdownOnSegmentation();
    this.cfg = options.toConfig()
      .setGridLogger(new VertxLogger());

    setNodeId(cfg);
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
  public void init(Vertx vertx, NodeSelector nodeSelector) {
    this.vertx = (VertxInternal) vertx;
    this.nodeSelector = nodeSelector;
  }

  @Override
  public void nodeListener(NodeListener nodeListener) {
    this.nodeListener = nodeListener;
  }

  @Override
  public <K, V> void getAsyncMap(String name, Promise<AsyncMap<K, V>> promise) {
    vertx.executeBlocking(prom -> prom.complete(new AsyncMapImpl<>(getCache(name), vertx)), promise);
  }

  @Override
  public <K, V> Map<K, V> getSyncMap(String name) {
    return new MapImpl<>(getCache(name));
  }

  @Override
  public void getLockWithTimeout(String name, long timeout, Promise<Lock> promise) {
    vertx.executeBlocking(prom -> {
      IgniteSemaphore semaphore = ignite.semaphore(LOCK_SEMAPHORE_PREFIX + name, 1, true, true);
      boolean locked;
      long remaining = timeout;
      do {
        long start = System.nanoTime();
        locked = semaphore.tryAcquire(remaining, TimeUnit.MILLISECONDS);
        remaining = remaining - TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS);
      } while (!locked && remaining > 0);
      if (locked) {
        prom.complete(new LockImpl(semaphore, lockReleaseExec));
      } else {
        throw new VertxException("Timed out waiting to get lock " + name);
      }
    }, false, promise);
  }

  @Override
  public void getCounter(String name, Promise<Counter> promise) {
    vertx.executeBlocking(prom -> prom.complete(new CounterImpl(ignite.atomicLong(name, 0, true))), promise);
  }

  @Override
  public String getNodeId() {
    return nodeId;
  }

  @Override
  public void setNodeInfo(NodeInfo nodeInfo, Promise<Void> promise) {
    synchronized (this) {
      this.nodeInfo = nodeInfo;
    }
    IgniteNodeInfo value = new IgniteNodeInfo(nodeInfo);
    vertx.executeBlocking(prom -> {
      nodeInfoMap.put(nodeId, value);
      prom.complete();
    }, false, promise);
  }

  @Override
  public synchronized NodeInfo getNodeInfo() {
    return nodeInfo;
  }

  @Override
  public void getNodeInfo(String id, Promise<NodeInfo> promise) {
    nodeInfoMap.getAsync(id).listen(fut -> {
      try {
        IgniteNodeInfo value = fut.get();
        if (value != null) {
          promise.complete(value.unwrap());
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
      return ignite.cluster().nodes().stream()
        .map(IgniteClusterManager::nodeId).collect(Collectors.toList());
    } catch (IllegalStateException e) {
      log.debug(e.getMessage());
      return Collections.emptyList();
    }
  }

  @Override
  public void join(Promise<Void> promise) {
    vertx.executeBlocking(prom -> {
      synchronized (monitor) {
        if (!active) {
          active = true;

          lockReleaseExec = Executors.newCachedThreadPool(r -> new Thread(r, "vertx-ignite-service-release-lock-thread"));

          if (!customIgnite) {
            cfg.setSegmentationPolicy(SegmentationPolicy.NOOP);
            cfg.setFailureHandler(new StopNodeFailureHandler());
            ignite = Ignition.start(cfg);
          }
          nodeId = nodeId(ignite.cluster().localNode());

          eventListener = event -> {
            if (!isActive()) {
              return false;
            }

            vertx.executeBlocking(f -> {
              String id = nodeId(((DiscoveryEvent) event).eventNode());
              switch (event.type()) {
                case EVT_NODE_JOINED:
                  if (nodeListener != null) {
                    nodeListener.nodeAdded(id);
                  }
                  log.debug("node " + id + " joined the cluster");
                  f.complete();
                  break;
                case EVT_NODE_LEFT:
                case EVT_NODE_FAILED:
                  if (cleanNodeInfos(id)) {
                    cleanSubs(id);
                  }
                  if (nodeListener != null) {
                    try {
                      nodeListener.nodeLeft(id);
                    } catch (Exception e) {
                      //ignore
                    }
                  }
                  log.debug("node " + id + " left the cluster");
                  f.complete();
                  break;
                case EVT_NODE_SEGMENTED:
                  if (customIgnite || !shutdownOnSegmentation) {
                    log.warn("node got segmented");
                  } else {
                    log.warn("node got segmented and will be shut down");
                    vertx.close();
                  }
                  f.fail(new IllegalStateException("node is stopped"));
                  break;
                default:
                  f.fail("event not known");
              }
            }, null);

            return true;
          };

          ignite.events().localListen(eventListener, IGNITE_EVENTS);
          subsMapHelper = new SubsMapHelper(ignite, nodeSelector, vertx);
          nodeInfoMap = ignite.getOrCreateCache("__vertx.nodeInfo");

          prom.complete();
        }
      }
    }, promise);
  }

  @Override
  public void leave(Promise<Void> promise) {
    vertx.executeBlocking(prom -> {
      synchronized (monitor) {
        if (active) {
          active = false;
          lockReleaseExec.shutdown();
          try {
            if (eventListener != null) {
              ignite.events().stopLocalListen(eventListener, IGNITE_EVENTS);
            }
            subsMapHelper.leave(ignite);
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

      prom.complete();
    }, promise);
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void addRegistration(String address, RegistrationInfo registrationInfo, Promise<Void> promise) {
    vertx.executeBlocking(prom -> {
      subsMapHelper.put(address, registrationInfo)
        .onComplete(prom);
    }, false, promise);
  }

  @Override
  public void removeRegistration(String address, RegistrationInfo registrationInfo, Promise<Void> promise) {
    vertx.executeBlocking(prom -> {
      subsMapHelper.remove(address, registrationInfo, prom);
    }, false, promise);
  }

  @Override
  public void getRegistrations(String address, Promise<List<RegistrationInfo>> promise) {
    vertx.executeBlocking(prom -> {
      subsMapHelper.get(address, prom);
    }, false, promise);
  }

  private void cleanSubs(String id) {
    try {
      subsMapHelper.removeAllForNode(id);
    } catch (IllegalStateException | CacheException e) {
      //ignore
    }
  }

  private boolean cleanNodeInfos(String nid) {
    try {
      return nodeInfoMap.remove(nid);
    } catch (IllegalStateException | CacheException e) {
      //ignore
    }
    return false;
  }

  private void setNodeId(IgniteConfiguration cfg) {
    UUID uuid = UUID.randomUUID();
    cfg.setNodeId(uuid);
    cfg.setIgniteInstanceName(VERTX_NODE_PREFIX + uuid);
  }

  private <K, V> IgniteCache<K, V> getCache(String name) {
    IgniteCache<K, V> cache = ignite.getOrCreateCache(name);
    return cache.withExpiryPolicy(DEFAULT_EXPIRY_POLICY);
  }

  private static String nodeId(ClusterNode node) {
    return node.id().toString();
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
      return vertx.executeBlocking(fut -> fut.complete(cnt.get()));
    }

    @Override
    public void get(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      get().onComplete(handler);
    }

    @Override
    public Future<Long> incrementAndGet() {
      return vertx.executeBlocking(fut -> fut.complete(cnt.incrementAndGet()));
    }

    @Override
    public void incrementAndGet(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      incrementAndGet().onComplete(handler);
    }

    @Override
    public Future<Long> getAndIncrement() {
      return vertx.executeBlocking(fut -> fut.complete(cnt.getAndIncrement()));
    }

    @Override
    public void getAndIncrement(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      getAndIncrement().onComplete(handler);
    }

    @Override
    public Future<Long> decrementAndGet() {
      return vertx.executeBlocking(fut -> fut.complete(cnt.decrementAndGet()));
    }

    @Override
    public void decrementAndGet(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      decrementAndGet().onComplete(handler);
    }

    @Override
    public Future<Long> addAndGet(long value) {
      return vertx.executeBlocking(fut -> fut.complete(cnt.addAndGet(value)));
    }

    @Override
    public void addAndGet(long value, Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      addAndGet(value).onComplete(handler);
    }

    @Override
    public Future<Long> getAndAdd(long value) {
      return vertx.executeBlocking(fut -> fut.complete(cnt.getAndAdd(value)));
    }

    @Override
    public void getAndAdd(long value, Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      getAndAdd(value).onComplete(handler);
    }

    @Override
    public Future<Boolean> compareAndSet(long expected, long value) {
      return vertx.executeBlocking(fut -> fut.complete(cnt.compareAndSet(expected, value)));
    }

    @Override
    public void compareAndSet(long expected, long value, Handler<AsyncResult<Boolean>> handler) {
      Objects.requireNonNull(handler, "handler");
      compareAndSet(expected, value).onComplete(handler);
    }
  }

  private static class ClearExpiryPolicy implements ExpiryPolicy, Serializable {
    @Override
    public Duration getExpiryForCreation() {
      return ETERNAL;
    }

    @Override
    public Duration getExpiryForAccess() {
      return ETERNAL;
    }

    @Override
    public Duration getExpiryForUpdate() {
      return ETERNAL;
    }
  }
}
