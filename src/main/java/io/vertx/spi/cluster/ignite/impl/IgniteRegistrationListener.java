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

package io.vertx.spi.cluster.ignite.impl;

import io.vertx.core.Handler;
import io.vertx.core.impl.TaskQueue;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.spi.cluster.RegistrationInfo;
import io.vertx.core.spi.cluster.RegistrationListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Lukas Prettenthaler
 */
public class IgniteRegistrationListener implements RegistrationListener {
  private interface InternalState {
    List<RegistrationInfo> initialState();

    void start();

    void stop();
  }

  private final VertxInternal vertx;
  private final SubsMapHelper helper;
  private final String address;
  private final AtomicReference<InternalState> internalState;

  private Handler<List<RegistrationInfo>> handler;
  private Handler<Void> endHandler;

  public IgniteRegistrationListener(VertxInternal vertx, SubsMapHelper helper, String address, List<RegistrationInfo> infos) {
    this.vertx = vertx;
    this.helper = helper;
    this.address = address;
    internalState = new AtomicReference<>(new IdleState(infos));
  }

  @Override
  public List<RegistrationInfo> initialState() {
    return internalState.get().initialState();
  }

  @Override
  public synchronized RegistrationListener handler(Handler<List<RegistrationInfo>> handler) {
    this.handler = handler;
    return this;
  }

  private synchronized Handler<List<RegistrationInfo>> getHandler() {
    return handler;
  }

  @Override
  public RegistrationListener exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public synchronized RegistrationListener endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  private synchronized Handler<Void> getEndHandler() {
    return endHandler;
  }

  @Override
  public void start() {
    internalState.get().start();
  }

  @Override
  public void stop() {
    internalState.get().stop();
  }

  private class IdleState implements InternalState {

    final List<RegistrationInfo> infos;

    IdleState(List<RegistrationInfo> infos) {
      this.infos = infos;
    }

    @Override
    public List<RegistrationInfo> initialState() {
      return infos;
    }

    @Override
    public void start() {
      StartedState startedState = new StartedState();
      if (internalState.compareAndSet(this, startedState)) {
        startedState.init(infos);
      }
    }

    @Override
    public void stop() {
      internalState.compareAndSet(this, new StoppedState());
    }
  }

  private class StartedState implements InternalState {

    final TaskQueue taskQueue = new TaskQueue();

    String listenerId;
    List<RegistrationInfo> initial, last;

    void init(List<RegistrationInfo> infos) {
      taskQueue.execute(() -> {
        if (this != internalState.get()) {
          return;
        }
        initial = infos;
        listenerId = helper.addEntryListener(address, this::subsChanged);
        subsChanged(); // make sure state is checked if entry is removed before listener is registered
      }, vertx.getWorkerPool());
    }

    void subsChanged() {
      taskQueue.execute(() -> {
        helper.get(address).onComplete(ar -> {
          if(ar.succeeded()) {
            handleDataUpdate(ar.result());
          }
        });
      }, vertx.getWorkerPool());
    }

    void handleDataUpdate(List<RegistrationInfo> infos) {
      if (this != internalState.get()) {
        return;
      }
      Runnable emission;
      if (initial != null) {
        if (infos.isEmpty()) {
          emission = terminalEvent();
        } else if (!initial.equals(infos)) {
          emission = itemEvent(infos);
        } else {
          emission = null;
        }
        last = infos;
        initial = null;
      } else if (last.isEmpty() || last.equals(infos)) {
        emission = null;
      } else {
        last = infos;
        if (last.isEmpty()) {
          emission = terminalEvent();
        } else {
          emission = itemEvent(infos);
        }
      }
      if (emission != null) {
        emission.run();
      }
    }

    private Runnable itemEvent(List<RegistrationInfo> infos) {
      Handler<List<RegistrationInfo>> h = getHandler();
      return () -> {
        if (h != null) {
          h.handle(infos);
        }
      };
    }

    private synchronized Runnable terminalEvent() {
      Handler<Void> e = getEndHandler();
      return () -> {
        stop();
        if (e != null) {
          e.handle(null);
        }
      };
    }

    @Override
    public List<RegistrationInfo> initialState() {
      return null;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
      if (internalState.compareAndSet(this, new StoppedState())) {
        taskQueue.execute(() -> {
          if (listenerId != null) {
            helper.removeEntryListener(listenerId);
          }
        }, vertx.getWorkerPool());
      }
    }
  }

  private class StoppedState implements InternalState {

    @Override
    public List<RegistrationInfo> initialState() {
      return null;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

  }
}
