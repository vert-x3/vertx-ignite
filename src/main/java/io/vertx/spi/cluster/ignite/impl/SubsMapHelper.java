/*
 * Copyright 2021 Red Hat, Inc.
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
package io.vertx.spi.cluster.ignite.impl;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.spi.cluster.RegistrationInfo;
import io.vertx.core.spi.cluster.RegistrationListener;
import io.vertx.core.spi.cluster.RegistrationUpdateEvent;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.binary.BinaryObjectException;
import org.apache.ignite.binary.BinaryReader;
import org.apache.ignite.binary.BinaryWriter;
import org.apache.ignite.binary.Binarylizable;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.ScanQuery;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.event.CacheEntryEvent;
import javax.cache.processor.MutableEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Thomas Segismont
 * @author Lukas Prettenthaler
 */
public class SubsMapHelper {
  private static final Logger log = LoggerFactory.getLogger(SubsMapHelper.class);
  private final IgniteCache<String, Set<IgniteRegistrationInfo>> map;
  private final RegistrationListener registrationListener;
  private final ConcurrentMap<String, Set<RegistrationInfo>> localSubs = new ConcurrentHashMap<>();
  private final Throttling throttling;
  private volatile boolean shutdown;

  public SubsMapHelper(Ignite ignite, RegistrationListener registrationListener, VertxInternal vertxInternal) {
    map = ignite.getOrCreateCache("__vertx.subs");
    this.registrationListener = registrationListener;
    throttling = new Throttling(vertxInternal, a -> getAndUpdate(a, vertxInternal));
    shutdown = false;
    map.query(new ContinuousQuery<String, Set<IgniteRegistrationInfo>>()
            .setAutoUnsubscribe(true)
            .setTimeInterval(100L)
            .setPageSize(128)
            .setLocalListener(l -> listen(l, vertxInternal)));
  }

  public List<RegistrationInfo> get(String address) {
    if (shutdown) {
      return Collections.emptyList();
    }
    try {
      Set<IgniteRegistrationInfo> remote = map.get(address);
      List<RegistrationInfo> infos;
      int size = (remote != null) ? remote.size() : 0;
      Set<RegistrationInfo> local = localSubs.get(address);
      if (local != null) {
        synchronized (local) {
          size += local.size();
          if (size == 0) {
            return Collections.emptyList();
          }
          infos = new ArrayList<>(size);
          infos.addAll(local);
        }
      } else if (size == 0) {
        return Collections.emptyList();
      } else {
        infos = new ArrayList<>(size);
      }
      if (remote != null) {
        for (IgniteRegistrationInfo info : remote) {
          infos.add(info.registrationInfo());
        }
      }

      return infos;
    } catch (IllegalStateException | CacheException e) {
      throw new VertxException(e, true);
    }
  }

  public Void put(String address, RegistrationInfo registrationInfo) {
    if (shutdown) {
      throw new VertxException("shutdown in progress");
    }
    try {
      if (registrationInfo.localOnly()) {
        localSubs.compute(address, (add, curr) -> addToSet(registrationInfo, curr));
        fireRegistrationUpdateEvent(address);
      } else {
        map.invoke(address, new AddRegistrationProcessor(new IgniteRegistrationInfo(address, registrationInfo)));
      }
    } catch (IllegalStateException | CacheException e) {
      throw new VertxException(e, true);
    }
    return null;
  }

  private Set<RegistrationInfo> addToSet(RegistrationInfo registrationInfo, Set<RegistrationInfo> curr) {
    Set<RegistrationInfo> res = curr != null ? curr : Collections.synchronizedSet(new LinkedHashSet<>());
    res.add(registrationInfo);
    return res;
  }

  public Void remove(String address, RegistrationInfo registrationInfo) {
    if (shutdown) {
      return null;
    }
    try {
      if (registrationInfo.localOnly()) {
        localSubs.computeIfPresent(address, (add, curr) -> removeFromSet(registrationInfo, curr));
        fireRegistrationUpdateEvent(address);
      } else {
        map.invoke(address, new RemoveRegistrationProcessor(new IgniteRegistrationInfo(address, registrationInfo)));
      }
    } catch (IllegalStateException | CacheException e) {
      throw new VertxException(e, true);
    }
    return null;
  }

  private Set<RegistrationInfo> removeFromSet(RegistrationInfo registrationInfo, Set<RegistrationInfo> curr) {
    curr.remove(registrationInfo);
    return curr.isEmpty() ? null : curr;
  }

  public void removeAllForNode(String nodeId) {
    try {
      List<Cache.Entry<String, Set<IgniteRegistrationInfo>>> allEntries = map
              .query(new ScanQuery<String, Set<IgniteRegistrationInfo>>(null))
              .getAll();
      if (allEntries == null || allEntries.isEmpty()) {
        return;
      }
      Set<String> keys = allEntries.stream()
              .map(Cache.Entry::getKey)
              .collect(Collectors.toSet());
      map.invokeAll(keys, new RemoveNodeRegistrationsProcessor(nodeId));
    } catch (IllegalStateException | CacheException t) {
      log.warn("Could not remove subscribers for nodeId " + nodeId + ": " + t.getMessage());
    }
  }

  public void leave() {
    shutdown = true;
  }

  private void fireRegistrationUpdateEvent(String address) {
    throttling.onEvent(address);
  }

  private Future<List<RegistrationInfo>> getAndUpdate(String address, VertxInternal vertxInternal) {
    Promise<List<RegistrationInfo>> prom = Promise.promise();
    if (registrationListener.wantsUpdatesFor(address)) {
      prom.future().onSuccess(registrationInfos -> {
        registrationListener.registrationsUpdated(new RegistrationUpdateEvent(address, registrationInfos));
      });
      vertxInternal.executeBlocking(() ->
              get(address), false
      ).onComplete(prom);
    } else {
      prom.complete();
    }
    return prom.future();
  }

  private void listen(final Iterable<CacheEntryEvent<? extends String, ? extends Set<IgniteRegistrationInfo>>> events, final VertxInternal vertxInternal) {
    vertxInternal.executeBlocking(() -> {
      StreamSupport.stream(events.spliterator(), false)
              .map(Cache.Entry::getKey)
              .distinct()
              .forEach(this::fireRegistrationUpdateEvent);
      return null;
    }, false);
  }

  private static class AddRegistrationProcessor implements CacheEntryProcessor<String, Set<IgniteRegistrationInfo>, Void>, Binarylizable {
    private static final long serialVersionUID = 1L;
    private IgniteRegistrationInfo info;

    AddRegistrationProcessor() {
    }

    AddRegistrationProcessor(IgniteRegistrationInfo info) {
      this.info = info;
    }

    @Override
    public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
      writer.writeObject("info", info);
    }

    @Override
    public void readBinary(BinaryReader reader) throws BinaryObjectException {
      info = reader.readObject("info");
    }

    @Override
    public Void process(MutableEntry<String, Set<IgniteRegistrationInfo>> entry, Object... arguments) {
      Set<IgniteRegistrationInfo> current = entry.getValue();
      if (current == null) {
        current = new HashSet<>();
      }
      current.add(info);
      entry.setValue(current);
      return null;
    }
  }

  private static class RemoveRegistrationProcessor implements CacheEntryProcessor<String, Set<IgniteRegistrationInfo>, Void>, Binarylizable {
    private static final long serialVersionUID = 1L;
    private IgniteRegistrationInfo info;

    RemoveRegistrationProcessor() {
    }

    RemoveRegistrationProcessor(IgniteRegistrationInfo info) {
      this.info = info;
    }

    @Override
    public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
      writer.writeObject("info", info);
    }

    @Override
    public void readBinary(BinaryReader reader) throws BinaryObjectException {
      info = reader.readObject("info");
    }

    @Override
    public Void process(MutableEntry<String, Set<IgniteRegistrationInfo>> entry, Object... arguments) {
      Set<IgniteRegistrationInfo> current = entry.getValue();
      if (current != null) {
        current.remove(info);
        if (current.isEmpty()) {
          entry.remove();
        } else {
          entry.setValue(current);
        }
      }
      return null;
    }
  }

  private static class RemoveNodeRegistrationsProcessor implements CacheEntryProcessor<String, Set<IgniteRegistrationInfo>, Void>, Binarylizable {
    private static final long serialVersionUID = 1L;
    private String nodeId;

    RemoveNodeRegistrationsProcessor() {
    }

    RemoveNodeRegistrationsProcessor(String nodeId) {
      this.nodeId = nodeId;
    }

    @Override
    public void writeBinary(BinaryWriter writer) throws BinaryObjectException {
      writer.writeString("nodeId", nodeId);
    }

    @Override
    public void readBinary(BinaryReader reader) throws BinaryObjectException {
      nodeId = reader.readString("nodeId");
    }

    @Override
    public Void process(MutableEntry<String, Set<IgniteRegistrationInfo>> entry, Object... arguments) {
      Set<IgniteRegistrationInfo> current = entry.getValue();
      if (current != null) {
        current.removeIf(info -> info.registrationInfo().nodeId().equals(nodeId));
        if (current.isEmpty()) {
          entry.remove();
        } else {
          entry.setValue(current);
        }
      }
      return null;
    }
  }
}
