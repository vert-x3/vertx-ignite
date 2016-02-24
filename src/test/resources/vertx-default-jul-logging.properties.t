# Copyright (c) 2015 The original author or authors
# ---------------------------------
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# and Apache License v2.0 which accompanies this distribution.
#
# The Eclipse Public License is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# The Apache License v2.0 is available at
# http://www.opensource.org/licenses/apache2.0.php
#
# You may elect to redistribute this code under either of these licenses.

handlers=java.util.logging.ConsoleHandler,java.util.logging.FileHandler
java.util.logging.SimpleFormatter.format=%5$s %6$s\n
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.FileHandler.level=INFO
java.util.logging.FileHandler.formatter=io.vertx.core.logging.impl.VertxLoggerFormatter

# Put the log in the system temporary directory
java.util.logging.FileHandler.pattern=%t/vertx.log

.level=INFO
io.vertx.level=INFO
org.apache.ignite.level=INFO
io.netty.util.internal.PlatformDependent.level=SEVERE