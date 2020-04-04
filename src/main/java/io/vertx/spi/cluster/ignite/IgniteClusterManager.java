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
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.spi.cluster.AsyncMultiMap;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.spi.cluster.NodeListener;
import io.vertx.spi.cluster.ignite.impl.AsyncMapImpl;
import io.vertx.spi.cluster.ignite.impl.AsyncMultiMapImpl;
import io.vertx.spi.cluster.ignite.impl.MapImpl;
import org.apache.ignite.*;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgnitePredicate;

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

  private Vertx vertx;

  private IgniteConfiguration cfg;
  private Ignite ignite;
  private boolean customIgnite;

  private String nodeID = UUID.randomUUID().toString();
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
    System.setProperty("IGNITE_NO_ASCII","true");
    System.setProperty("IGNITE_QUIET","true");
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
    System.setProperty("IGNITE_PERFORMANCE_SUGGESTIONS_DISABLED", "true");
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
    setNodeID(cfg);
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
  public void setVertx(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void nodeListener(NodeListener nodeListener) {
    this.nodeListener = nodeListener;
  }

  @Override
  public <K, V> void getAsyncMultiMap(String name, Handler<AsyncResult<AsyncMultiMap<K, V>>> handler) {
    vertx.executeBlocking(
      fut -> fut.complete(new AsyncMultiMapImpl<>(this.<K, Set<V>>getCache(name), vertx)), handler
    );
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
  public String getNodeID() {
    return nodeID;
  }

  @Override
  public List<String> getNodes() {
    return ignite.cluster().nodes().stream()
      .map(IgniteClusterManager::nodeId).collect(Collectors.toList());
  }

  @Override
  public void join(Handler<AsyncResult<Void>> handler) {
    synchronized (monitor) {
      vertx.executeBlocking(fut -> {
        if (!active) {
          active = true;

          lockReleaseExec = Executors.newCachedThreadPool(r -> new Thread(r, "vertx-ignite-service-release-lock-thread"));

          if (!customIgnite) {
            ignite = cfg == null ? Ignition.start(loadConfiguration()) : Ignition.start(cfg);
          }
          nodeID = nodeId(ignite.cluster().localNode());

          eventListener = event -> {
            if (!active) {
              return false;
            }

            if (nodeListener != null) {
              vertx.executeBlocking(f -> {
                if (isActive()) {
                  switch (event.type()) {
                    case EVT_NODE_JOINED:
                      nodeListener.nodeAdded(nodeId(((DiscoveryEvent)event).eventNode()));
                      break;
                    case EVT_NODE_LEFT:
                    case EVT_NODE_FAILED:
                      String nodeId = nodeId(((DiscoveryEvent)event).eventNode());
                      nodeListener.nodeLeft(nodeId);
                      break;
                  }
                }
                f.complete();
              }, null);
            }

            return true;
          };

          ignite.events().localListen(eventListener, EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED);

          fut.complete();
        }
      }, handler);
    }
  }

  @Override
  public void leave(Handler<AsyncResult<Void>> handler) {
    synchronized (monitor) {
      vertx.executeBlocking(fut -> {
        if (active) {
          active = false;
          lockReleaseExec.shutdown();
          try {
            if (!customIgnite)
              ignite.close();
            else if (eventListener != null)
              ignite.events().stopLocalListen(eventListener, EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED);
          } catch (Exception e) {
            log.error(e);
          }
        }

        fut.complete();
      }, handler);
    }
  }

  @Override
  public boolean isActive() {
    return active;
  }

  private IgniteConfiguration loadConfiguration(URL config) {
    try {
      IgniteConfiguration cfg = F.first(IgnitionEx.loadConfigurations(config).get1());
      setNodeID(cfg);
      return cfg;
    } catch (IgniteCheckedException e) {
      log.error("Configuration loading error:", e);
      throw new RuntimeException(e);
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
      setNodeID(cfg);
      return cfg;
    } catch (IgniteCheckedException e) {
      log.error("Configuration loading error:", e);
      throw new RuntimeException(e);
    }
  }

  private void setNodeID(IgniteConfiguration cfg) {
    UUID uuid = UUID.fromString(nodeID);
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

  private class LockImpl implements Lock {
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
