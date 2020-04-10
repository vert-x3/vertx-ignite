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
import io.vertx.core.Future;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.spi.cluster.*;
import io.vertx.spi.cluster.ignite.impl.*;
import org.apache.ignite.*;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgnitePredicate;

import javax.cache.CacheException;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static javax.cache.expiry.Duration.ETERNAL;
import static org.apache.ignite.events.EventType.*;

/**
 * Apache Ignite based cluster manager.
 *
 * @author Andrey Gura
 */
public class IgniteClusterManager implements ClusterManager {

  private static final Logger log = LoggerFactory.getLogger(IgniteClusterManager.class);

  // Default Ignite configuration file
  private static final String DEFAULT_CONFIG_FILE = "default-ignite.xml";

  // User defined Ignite configuration file
  private static final String CONFIG_FILE = "ignite.xml";

  private static final String VERTX_NODE_PREFIX = "vertx.ignite.node.";

  private static final String LOCK_SEMAPHORE_PREFIX = "__vertx.";

  // Workaround for https://github.com/vert-x3/vertx-ignite/issues/63
  private static final ExpiryPolicy DEFAULT_EXPIRY_POLICY = new ClearExpiryPolicy();

  private VertxInternal vertx;

  private IgniteConfiguration cfg;
  private Ignite ignite;
  private boolean customIgnite;

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
    System.setProperty("IGNITE_NO_ASCII", "true");
    System.setProperty("IGNITE_QUIET", "false");
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
    System.setProperty("IGNITE_PERFORMANCE_SUGGESTIONS_DISABLED", "true");
    System.setProperty("IGNITE_UPDATE_NOTIFIER", "false");
  }

  /**
   * Creates cluster manager instance with given Ignite configuration.
   * Use this constructor in order to configure cluster manager programmatically.
   *
   * @param cfg {@code IgniteConfiguration} instance.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager(IgniteConfiguration cfg) {
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
    this.cfg = loadConfiguration(configFile);
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
  public void setVertx(VertxInternal vertx) {
    this.vertx = vertx;
  }

  @Override
  public void nodeListener(NodeListener nodeListener) {
    this.nodeListener = nodeListener;
  }

  @Override
  public <K, V> Future<AsyncMap<K, V>> getAsyncMap(String name) {
    return vertx.executeBlocking(fut -> fut.complete(new AsyncMapImpl<>(getCache(name), vertx)));
  }

  @Override
  public <K, V> Map<K, V> getSyncMap(String name) {
    return new MapImpl<>(getCache(name));
  }

  @Override
  public Future<Lock> getLockWithTimeout(String name, long timeout) {
    return vertx.executeBlocking(fut -> {
      IgniteSemaphore semaphore = ignite.semaphore(LOCK_SEMAPHORE_PREFIX + name, 1, true, true);
      boolean locked;
      long remaining = timeout;
      do {
        long start = System.nanoTime();
        locked = semaphore.tryAcquire(remaining, TimeUnit.MILLISECONDS);
        remaining = remaining - TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, NANOSECONDS);
      } while (!locked && remaining > 0);
      if (locked) {
        fut.complete(new LockImpl(semaphore, lockReleaseExec));
      } else {
        throw new VertxException("Timed out waiting to get lock " + name);
      }
    }, false);
  }

  @Override
  public Future<Counter> getCounter(String name) {
    return vertx.executeBlocking(fut -> fut.complete(new CounterImpl(ignite.atomicLong(name, 0, true))));
  }

  @Override
  public String getNodeId() {
    return nodeId;
  }

  @Override
  public Future<Void> setNodeInfo(NodeInfo nodeInfo) {
    synchronized (this) {
      this.nodeInfo = nodeInfo;
    }
    IgniteNodeInfo value = new IgniteNodeInfo(nodeInfo);
    return vertx.executeBlocking(promise -> {
      nodeInfoMap.put(nodeId, value);
      promise.complete();
    }, false);
  }

  @Override
  public synchronized NodeInfo getNodeInfo() {
    return nodeInfo;
  }

  @Override
  public Future<NodeInfo> getNodeInfo(String id) {
    ContextInternal ctx = vertx.getOrCreateContext();
    Promise<NodeInfo> promise = ctx.promise();
    nodeInfoMap.getAsync(id).listen(fut -> {
      try {
        IgniteNodeInfo value = fut.get();
        promise.complete(value != null ? value.unwrap() : null);
      } catch (IgniteException e) {
        promise.fail(e);
      }
    });
    return promise.future();
  }

  @Override
  public List<String> getNodes() {
    try {
      return ignite.cluster().nodes().stream()
        .map(IgniteClusterManager::nodeId).collect(Collectors.toList());
    } catch (IllegalStateException e) {
      log.debug(e.getMessage());
      return new ArrayList<>();
    }
  }

  @Override
  public Future<Void> join() {
    return vertx.executeBlocking(fut -> {
      synchronized (monitor) {
        if (!active) {
          active = true;

          lockReleaseExec = Executors.newCachedThreadPool(r -> new Thread(r, "vertx-ignite-service-release-lock-thread"));

          if (!customIgnite) {
            ignite = cfg == null ? Ignition.start(loadConfiguration()) : Ignition.start(cfg);
          }
          nodeId = nodeId(ignite.cluster().localNode());

          eventListener = event -> {
            if (!active) {
              return false;
            }

            vertx.executeBlocking(f -> {
              if (isActive()) {
                switch (event.type()) {
                  case EVT_NODE_JOINED:
                    if (nodeListener != null) {
                      nodeListener.nodeAdded(nodeId(((DiscoveryEvent) event).eventNode()));
                    }
                    break;
                  case EVT_NODE_LEFT:
                  case EVT_NODE_FAILED:
                    String id = nodeId(((DiscoveryEvent) event).eventNode());
                    if (isMaster()) {
                      cleanSubs(id);
                      cleanNodeInfos(id);
                    }
                    if (nodeListener != null) {
                      nodeListener.nodeLeft(id);
                    }
                    break;
                }
              }
              f.complete();
            }, null);

            return true;
          };

          ignite.events().localListen(eventListener, EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED);
          subsMapHelper = new SubsMapHelper(ignite);
          nodeInfoMap = ignite.getOrCreateCache("__vertx.nodeInfo");

          fut.complete();
        }
      }
    });
  }

  @Override
  public Future<Void> leave() {
    return vertx.executeBlocking(fut -> {
      synchronized (monitor) {
        if (active) {
          active = false;
          lockReleaseExec.shutdown();
          try {
            if (eventListener != null) {
              ignite.events().stopLocalListen(eventListener, EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED);
            }
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

      fut.complete();
    });
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public Future<Void> register(String address, RegistrationInfo registrationInfo) {
    return vertx.executeBlocking(promise -> {
      subsMapHelper.put(address, registrationInfo)
        .onComplete(promise);
    }, false);
  }

  @Override
  public Future<Void> unregister(String address, RegistrationInfo registrationInfo) {
    return vertx.executeBlocking(promise -> {
      subsMapHelper.remove(address, registrationInfo)
        .onComplete(promise);
    }, false);
  }

  @Override
  public Future<RegistrationListener> registrationListener(String address) {
    return vertx.executeBlocking(promise -> {
      subsMapHelper.get(address)
        .map(infos -> (RegistrationListener) new IgniteRegistrationListener(vertx, subsMapHelper, address, infos))
        .onComplete(promise);
    }, false);
  }

  private IgniteConfiguration loadConfiguration(URL config) {
    try {
      IgniteConfiguration cfg = F.first(IgnitionEx.loadConfigurations(config).get1());
      setNodeId(cfg);
      return cfg;
    } catch (IgniteCheckedException e) {
      log.error("Configuration loading error:", e);
      throw new VertxException(e);
    }
  }

  private IgniteConfiguration loadConfiguration() {
    ClassLoader ctxClsLoader = Thread.currentThread().getContextClassLoader();

    InputStream is = null;

    if (ctxClsLoader != null) {
      is = ctxClsLoader.getResourceAsStream(CONFIG_FILE);
    }

    if (is == null) {
      is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);

      if (is == null) {
        is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);
        log.info("Using default configuration.");
      }
    }

    try {
      IgniteConfiguration cfg = F.first(IgnitionEx.loadConfigurations(is).get1());
      setNodeId(cfg);
      return cfg;
    } catch (IgniteCheckedException e) {
      log.error("Configuration loading error:", e);
      throw new VertxException(e);
    }
  }

  private boolean isMaster() {
    return nodeId(ignite.cluster()
      .forOldest().node())
      .equals(nodeId);
  }

  private void cleanSubs(String id) {
    try {
      subsMapHelper.removeAllForNode(id);
    } catch (IllegalStateException | CacheException e) {
      //ignore
    }
  }

  private void cleanNodeInfos(String nid) {
    try {
      nodeInfoMap.remove(nid);
    } catch (IllegalStateException | CacheException e) {
      //ignore
    }
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
