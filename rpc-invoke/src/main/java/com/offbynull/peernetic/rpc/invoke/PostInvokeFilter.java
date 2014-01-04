/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.rpc.invoke;

import org.apache.commons.lang3.Validate;

/**
 * Inspects and potentially makes changes to an incoming invocation's result/exception.
 * @author Kasra Faghihi
 */
public interface PostInvokeFilter {
    /**
     * Validates/inspects/modifies incoming invocation's result/exception.
     * @param result invocation result (must never be {@code null})
     * @throws RuntimeException on validation error
     * @throws NullPointerException if any arguments are {@code null}
     * @return modified invocation result/exception
     */
    Result filter(Result result);
    
    /**
     * Result type.
     */
    public enum ResultType {
        /**
         * Method threw an exception.
         */
        THROW,
        /**
         * Method returned a value (possibly {@code null}).
         */
        RETURN
    }
    
    /**
     * Encapsulates a result/exception from a method invocation.
     */
    final class Result {
        private ResultType type;
        private Object result;

        /**
         * Constructs a {@link Result} object.
         * @param type result type
         * @param result result object -- if {@code type} is {@link ResultType#THROW} then this must be a type derived from
         * {@link Exception}
         * @throws NullPointerException if (@code type} is {@code null}, or if {@link ResultType#THROW} but {@code result} is {@code null}
         * @throws IllegalArgumentException if {@code result} is {@link ResultType#THROW} but {@code result} is not a type derived from
         * {@link Exception}
         */
        public Result(ResultType type, Object result) {
            Validate.notNull(type);
            
            switch (type) {
                case RETURN:
                    break;
                case THROW:
                    Validate.notNull(result);
                    Validate.isTrue(result instanceof Exception);
                    break;
                default:
                    throw new IllegalArgumentException(); // should never happen
            }

            this.type = type;
            this.result = result;
        }

        /**
         * Get the result type.
         * @return result type
         */
        public ResultType getType() {
            return type;
        }

        /**
         * Get the result object.
         * @return result object
         */
        public Object getResult() {
            return result;
        }
        
    }
}
