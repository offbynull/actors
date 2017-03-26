/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.actors.core.gateways.log;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

/**
 * Message passed to {@link LogGateway} to log a message.
 * <p>
 * Use one of the construction methods to create an instance of this message: {@link #trace(java.lang.String, java.lang.Object...) },
 * {@link #debug(java.lang.String, java.lang.Object...) }, {@link #info(java.lang.String, java.lang.Object...) },
 * {@link #warn(java.lang.String, java.lang.Object...) }, or {@link #error(java.lang.String, java.lang.Object...) }. Usage pattern of
 * construction methods follow the same usage pattern as SLF4J's {@link Logger}. That is, you pass arguments and make use of them the same
 * way as you would to SLF4J's {@link Logger}.
 * <p>
 * Here's an example:
 * <pre>
 * LogMessage.info("This is an info msg: {} {}", "test arg 1", 2)
 * </pre>
 * The code above constructs a {@link LogMessage} object that specifies that an INFO-level log message should be created with the text
 * "{@code This is an info msg: test arg 1 2}".
 * @author Kasra Faghihi
 */
public final class LogMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private static final String NULL_STRING = "null";

    private Type type;
    private String message;
    private UnmodifiableList<String> arguments;

    private LogMessage(Type type, String message, Object... arguments) {
        Validate.notNull(type);
        Validate.notNull(message);
        Validate.notNull(arguments); // arguments can contain null elements

        this.type = type;
        this.message = message;

        List<String> args = Arrays.stream(arguments) // convert args to strings here on purpose -- objects may not be immutable/serializable
                .map(x -> {
                    if (x == null) {
                        return NULL_STRING;
                    } else if (x instanceof Throwable) {
                        return ExceptionUtils.getStackTrace((Throwable) x);
                    } else {
                        return x.toString();
                    }
                })
                .collect(Collectors.toList());
        this.arguments = (UnmodifiableList<String>) UnmodifiableList.unmodifiableList(args);
    }
    
    // IMPORTANT READ ME!!!!!!!
    //
    // Construction methods take in Object array of args, but that object array has every element converted to a string. As such, this
    // class meets requirements of messages being immutable and serializable!

    /**
     * Constructs a {@link LogMessage} instance that's indicates a TRACE-level log message is to be piped to SLF4J.
     * @param message message to be logged
     * @param arguments arguments to insert in to {@code message}
     * @return new {@link LogMessage} instance
     * @throws NullPointerException if any argument is {@code null} (note that {@code arguments} can contain {@code null}s)
     */
    public static LogMessage trace(String message, Object... arguments) {
        return new LogMessage(Type.TRACE, message, arguments);
    }

    /**
     * Constructs a {@link LogMessage} instance that's indicates a DEBUG-level log message is to be piped to SLF4J.
     * @param message message to be logged
     * @param arguments arguments to insert in to {@code message}
     * @return new {@link LogMessage} instance
     * @throws NullPointerException if any argument is {@code null} (note that {@code arguments} can contain {@code null}s)
     */
    public static LogMessage debug(String message, Object... arguments) {
        return new LogMessage(Type.DEBUG, message, arguments);
    }

    /**
     * Constructs a {@link LogMessage} instance that's indicates a INFO-level log message is to be piped to SLF4J.
     * @param message message to be logged
     * @param arguments arguments to insert in to {@code message}
     * @return new {@link LogMessage} instance
     * @throws NullPointerException if any argument is {@code null} (note that {@code arguments} can contain {@code null}s)
     */
    public static LogMessage info(String message, Object... arguments) {
        return new LogMessage(Type.INFO, message, arguments);
    }

    /**
     * Constructs a {@link LogMessage} instance that's indicates a WARN-level log message is to be piped to SLF4J.
     * @param message message to be logged
     * @param arguments arguments to insert in to {@code message}
     * @return new {@link LogMessage} instance
     * @throws NullPointerException if any argument is {@code null} (note that {@code arguments} can contain {@code null}s)
     */
    public static LogMessage warn(String message, Object... arguments) {
        return new LogMessage(Type.WARN, message, arguments);
    }

    /**
     * Constructs a {@link LogMessage} instance that's indicates a ERROR-level log message is to be piped to SLF4J.
     * @param message message to be logged
     * @param arguments arguments to insert in to {@code message}
     * @return new {@link LogMessage} instance
     * @throws NullPointerException if any argument is {@code null} (note that {@code arguments} can contain {@code null}s)
     */
    public static LogMessage error(String message, Object... arguments) {
        return new LogMessage(Type.ERROR, message, arguments);
    }

    Type getType() {
        return type;
    }

    String getMessage() {
        return message;
    }

    Object[] getArguments() {
        return arguments.toArray();
    }

    enum Type {

        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
