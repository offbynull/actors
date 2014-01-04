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

/**
 * Provides the ability to proxy a asynchronous interface such that method invocations are processed by some external source. This class
 * essentially does what {@link Capturer} does, but does so using an asynchronous interface. That is, this class expects to proxy an
 * interface where the method signatures of the interface resemble that of a non-final class/interface that you would pass in to
 * {@link Capturer}. The difference between the method signatures are that..
 * <ol>
 * <li>Return type must be void.</li>
 * <li>An extra parameter of type {@link AsyncResultListener} must be added in as the first parameter.</li>
 * </ol>
 * The return value / thrown exception will be passed back to the AsyncResultListener object passed in to the first argument.
 * <p/>
 * Example...
 * <p/>
 * Original interface for {@link Capturer}:
 * <pre>
 * public void MyServiceClass {
 *    String performFunction(int id);
 * }
 * </pre>
 * <p/>
 * Async interface for this class ({@link CglibAsyncCapturer}):
 * <pre>
  * public void MyServiceClass {
 *    void performFunction(AsyncResultListener<String> result, int id);
 * }
 * </pre>
 * @author Kasra Faghihi
 * @param <T> proxy type
 * @param <AT> proxy async type
 */
public interface AsyncCapturer<T, AT> {

    /**
     * Creates an async proxy object.
     * @param callback callback to notify when a method's been invoked on the returned proxy object
     * @return proxy object
     * @throws NullPointerException if any arguments are {@code null}
     */
    AT createInstance(final AsyncCapturerHandler callback);
    
}
