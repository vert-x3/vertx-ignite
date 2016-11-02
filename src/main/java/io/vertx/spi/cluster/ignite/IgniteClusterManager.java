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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.spi.cluster.AsyncMultiMap;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.core.spi.cluster.NodeListener;
import io.vertx.spi.cluster.ignite.impl.AsyncMapImpl;
import io.vertx.spi.cluster.ignite.impl.AsyncMultiMapImpl;
import io.vertx.spi.cluster.ignite.impl.MapImpl;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.util.typedef.F;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.ignite.events.EventType.EVT_NODE_FAILED;
import static org.apache.ignite.events.EventType.EVT_NODE_JOINED;
import static org.apache.ignite.events.EventType.EVT_NODE_LEFT;

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

  public static final String VERTX_CACHE_TEMPLATE_NAME = "*";

  private static final String VERTX_NODE_PREFIX = "vertx.ignite.node.";

  private final Queue<String> pendingLocks = new ConcurrentLinkedQueue<>();

  private Vertx vertx;

  private IgniteConfiguration cfg;
  private Ignite ignite;

  private String nodeID = UUID.randomUUID().toString();
  private NodeListener nodeListener;

  private volatile boolean active;

  private final Object monitor = new Object();

  private CollectionConfiguration collectionCfg;

  /**
   * Default constructor. Cluster manager will get configuration from classpath.
   */
  @SuppressWarnings("unused")
  public IgniteClusterManager() {
    System.setProperty("IGNITE_NO_SHUTDOWN_HOOK", "true");
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
      fut -> fut.complete(new AsyncMultiMapImpl<>(this.<K, List<V>>getCache(name), vertx)), handler
    );
  }

  @Override
  public <K, V> void getAsyncMap(String name, Handler<AsyncResult<AsyncMap<K, V>>> handler) {
    vertx.executeBlocking(
      fut -> fut.complete(new AsyncMapImpl<>(getCache(name), vertx)), handler
    );
  }

  @Override
  public <K, V> Map<K, V> getSyncMap(String name) {
    return new MapImpl<>(getCache(name));
  }

  @Override
  public void getLockWithTimeout(String name, long timeout, Handler<AsyncResult<Lock>> handler) {
    vertx.executeBlocking(fut -> {
      boolean locked = false;

      try {
        IgniteQueue<String> queue = getQueue(name, true);

        pendingLocks.offer(name);

        locked = queue.offer(getNodeID(), timeout, TimeUnit.MILLISECONDS);

        if (!locked) {
          // EVT_NODE_LEFT/EVT_NODE_FAILED event might be already handled, so trying get lock again if
          // node left topology.
          // Use IgniteSempahore when it will be fixed.
          String ownerId = queue.peek();
          ClusterNode ownerNode = ignite.cluster().forNodeId(UUID.fromString(ownerId)).node();
          if (ownerNode == null) {
            queue.remove(ownerId);
            locked = queue.offer(getNodeID(), timeout, TimeUnit.MILLISECONDS);
          }
        }
      } catch (Exception e) {
        fut.fail(new VertxException("Error during getting lock " + name, e));
      } finally {
        pendingLocks.remove(name);
      }

      if (locked) {
        fut.complete(new LockImpl(name));
      } else {
        fut.fail(new VertxException("Timed out waiting to get lock " + name));
      }
    }, handler);
  }

  @Override
  public void getCounter(String name, Handler<AsyncResult<Counter>> handler) {
    vertx.executeBlocking(fut -> fut.complete(new CounterImpl(ignite.atomicLong(name, 0, true))), handler);
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

          ignite = cfg == null ? Ignition.start(loadConfiguration()) : Ignition.start(cfg);
          nodeID = nodeId(ignite.cluster().localNode());

          for (CacheConfiguration cacheCfg : ignite.configuration().getCacheConfiguration()) {
            if (cacheCfg.getName().equals(VERTX_CACHE_TEMPLATE_NAME)) {
              collectionCfg = new CollectionConfiguration();
              collectionCfg.setAtomicityMode(cacheCfg.getAtomicityMode());
              collectionCfg.setBackups(cacheCfg.getBackups());
              break;
            }
          }

          if (collectionCfg == null) {
            collectionCfg = new CollectionConfiguration();
          }

          ignite.events().localListen(event -> {
            if (!active) {
              return false;
            }

            if (nodeListener != null) {
              vertx.executeBlocking(f -> {
                if (isActive()) {
                  switch (event.type()) {
                    case EVT_NODE_JOINED:
                      nodeListener.nodeAdded(nodeId(((DiscoveryEvent) event).eventNode()));
                      break;
                    case EVT_NODE_LEFT:
                    case EVT_NODE_FAILED:
                      String nodeId = nodeId(((DiscoveryEvent)event).eventNode());
                      nodeListener.nodeLeft(nodeId);
                      releasePendingLocksForFailedNode(nodeId);
                      break;
                  }
                }
                fut.complete();
              }, null);
            }

            return true;
          }, EVT_NODE_JOINED, EVT_NODE_LEFT, EVT_NODE_FAILED);

          fut.complete();
        }
      }, handler);
    }
  }

  /**
   * @param nodeId ID of node that left topology
   */
  private void releasePendingLocksForFailedNode(final String nodeId) {
    Set<String> processed = new HashSet<>();

    pendingLocks.forEach(new Consumer<String>() {
      @Override public void accept(String name) {
        if (processed.add(name)) {
          IgniteQueue<String> queue = getQueue(name, false);

          if (queue != null && nodeId.equals(queue.peek())) {
            queue.remove(nodeId);
          }
        }
      }
    });
  }

  @Override
  public void leave(Handler<AsyncResult<Void>> handler) {
    synchronized (monitor) {
      vertx.executeBlocking(fut -> {
        if (active) {
          active = false;
          try {
            ignite.close();
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
    cfg.setGridName(VERTX_NODE_PREFIX + uuid);
  }

  private <K, V> IgniteCache<K, V> getCache(String name) {
    return ignite.getOrCreateCache(name);
  }

  private <T> IgniteQueue<T> getQueue(String name, boolean create) {
    return ignite.queue(name, 1, create ? collectionCfg : null);
  }

  private static String nodeId(ClusterNode node) {
    return node.id().toString();
  }

  private class LockImpl implements Lock {
    private final String name;

    private LockImpl(String name) {
      this.name = name;
    }

    @Override
    public void release() {
      IgniteQueue<String> queue = getQueue(name, true);
      String ownerId = queue.poll();

      if (ownerId == null) {
        throw new VertxException("Inconsistent lock state " + name);
      }
    }
  }

  private class CounterImpl implements Counter {
    private final IgniteAtomicLong cnt;

    private CounterImpl(IgniteAtomicLong cnt) {
      this.cnt = cnt;
    }

    @Override
    public void get(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      vertx.executeBlocking(fut -> fut.complete(cnt.get()), handler);
    }

    @Override
    public void incrementAndGet(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      vertx.executeBlocking(fut -> fut.complete(cnt.incrementAndGet()), handler);
    }

    @Override
    public void getAndIncrement(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      vertx.executeBlocking(fut -> fut.complete(cnt.getAndIncrement()), handler);
    }

    @Override
    public void decrementAndGet(Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      vertx.executeBlocking(fut -> fut.complete(cnt.decrementAndGet()), handler);
    }

    @Override
    public void addAndGet(long value, Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      vertx.executeBlocking(fut -> fut.complete(cnt.addAndGet(value)), handler);
    }

    @Override
    public void getAndAdd(long value, Handler<AsyncResult<Long>> handler) {
      Objects.requireNonNull(handler, "handler");
      vertx.executeBlocking(fut -> fut.complete(cnt.getAndAdd(value)), handler);
    }

    @Override
    public void compareAndSet(long expected, long value, Handler<AsyncResult<Boolean>> handler) {
      Objects.requireNonNull(handler, "handler");
      vertx.executeBlocking(fut -> fut.complete(cnt.compareAndSet(expected, value)), handler);
    }
  }
}
