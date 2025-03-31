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
import io.vertx.core.spi.cluster.RegistrationInfo;
import io.vertx.core.spi.cluster.RegistrationListener;
import io.vertx.core.spi.cluster.RegistrationUpdateEvent;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.ScanQuery;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.event.CacheEntryEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * @author Thomas Segismont
 * @author Lukas Prettenthaler
 */
public class SubsMapHelper {
  private final IgniteCache<IgniteRegistrationInfo, Boolean> map;
  private final RegistrationListener registrationListener;
  private final ConcurrentMap<String, Set<RegistrationInfo>> localSubs = new ConcurrentHashMap<>();
  private final Throttling throttling;
  private volatile boolean shutdown;

  public SubsMapHelper(Ignite ignite, RegistrationListener registrationListener, VertxInternal vertxInternal) {
    map = ignite.getOrCreateCache("__vertx.subs");
    this.registrationListener = registrationListener;
    throttling = new Throttling(vertxInternal, a -> getAndUpdate(a, vertxInternal));
    shutdown = false;
    map.query(new ContinuousQuery<IgniteRegistrationInfo, Boolean>()
      .setAutoUnsubscribe(true)
      .setTimeInterval(100L)
      .setPageSize(128)
      .setLocalListener(l -> listen(l, vertxInternal)));
  }

  public List<RegistrationInfo> get(String address) {
    if (shutdown) {
      return null;
    }
    try {
      List<RegistrationInfo> infos = map.query(new ScanQuery<IgniteRegistrationInfo, Boolean>((k, v) -> k.address().equals(address)))
        .getAll().stream()
        .map(Cache.Entry::getKey)
        .map(IgniteRegistrationInfo::registrationInfo)
        .collect(toList());
      int size = infos.size();
      Set<RegistrationInfo> local = localSubs.get(address);
      if (local != null) {
        synchronized (local) {
          size += local.size();
          if (size == 0) {
            return Collections.emptyList();
          }
          infos.addAll(local);
        }
      } else if (size == 0) {
        return Collections.emptyList();
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
        map.put(new IgniteRegistrationInfo(address, registrationInfo), Boolean.TRUE);
      }
    } catch (IllegalStateException | CacheException e) {
      throw new VertxException(e);
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
        map.remove(new IgniteRegistrationInfo(address, registrationInfo));
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
    TreeSet<IgniteRegistrationInfo> toRemove = map.query(new ScanQuery<IgniteRegistrationInfo, Boolean>((k, v) -> k.registrationInfo().nodeId().equals(nodeId)))
      .getAll().stream()
      .map(Cache.Entry::getKey)
      .collect(Collectors.toCollection(TreeSet::new));
    try {
      map.removeAll(toRemove);
    } catch (IllegalStateException | CacheException t) {
      //ignore
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

  private void listen(final Iterable<CacheEntryEvent<? extends IgniteRegistrationInfo, ? extends Boolean>> events, final VertxInternal vertxInternal) {
    vertxInternal.executeBlocking(() -> {
      StreamSupport.stream(events.spliterator(), false)
        .map(e -> e.getKey().address())
        .distinct()
        .forEach(this::fireRegistrationUpdateEvent);
      return null;
    }, false);
  }
}
