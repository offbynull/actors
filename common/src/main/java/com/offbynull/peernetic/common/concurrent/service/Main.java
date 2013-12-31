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
package com.offbynull.peernetic.common.concurrent.service;

/**
 * Test class.
 *
 * @author Kasra Faghihi
 */
public final class Main {

    private Main() {

    }

    /**
     * Test.
     *
     * @param args unused
     * @throws Throwable error
     */
    public static void main(String[] args) throws Throwable {

        Service serviceThread = new Service("New Service!", true) {

            @Override
            protected void onStop() throws Exception {
                System.out.println("onStop");
            }

            @Override
            protected void onProcess() throws Exception {
                System.out.println("onProcess");
            }

            @Override
            protected void onStart(Object... init) throws Exception {
                System.out.println("onStart");
            }

        };

        serviceThread.start();
        serviceThread.stop();
    }
}
