/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.examples.test;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.log.LogMessage;
import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;

public class Test {

    public static void main(String[] args) throws Exception {
        helloWorldTest();
//        cpuTest();
        //basicTest();
        //basicTimer();
    }

    private static void helloWorldTest() throws InterruptedException {
        // Create coroutine actor that forwards messages to the logger
        Coroutine echoerActor = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            
            // First message is the priming message. It should be the address to the logger.
            final Address loggerAddress = (Address) ctx.in();
            cnt.suspend();

            // All messages after the first message are messages that we should log and echo back.
            do {
                Object msg = ctx.in();
                Address srcAddress = ctx.source();
                ctx.out(loggerAddress, LogMessage.debug("Received a message: {}", msg));
                ctx.out(srcAddress, "Echoing back " + msg);
                cnt.suspend();
            } while (true);
        };

        // Create the actor runner, logger gateway, and direct gateway.
        ActorRunner actorRunner = new ActorRunner("actors"); // container for actors
        LogGateway logGateway = new LogGateway("log"); // gateway that logs to slf4j
        DirectGateway directGateway = new DirectGateway("direct"); // gateway that allows allows interfacing with actors/gateways from normal java code

        // Allow the actor runner to send messages to the log gateway
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());
        
        // Allow the actor runner and the direct gateway to send messages to eachother
        actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
        directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());

        // Add the coroutine actor and prime it with a hello world message
        actorRunner.addActor("echoer", echoerActor, Address.of("log"));

        
        Scanner inScanner = new Scanner(System.in);
        while(inScanner.hasNextLine()) {
            // Read next line and forward to actor
            String input = inScanner.nextLine();
            directGateway.writeMessage(Address.fromString("actors:echoer"), input);
            
            // Wait for response from actor and print out
            String response = (String) directGateway.readMessages().get(0).getMessage();
            System.out.println(response);
        }
    }

    private static void cpuTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ActorRunner runner = new ActorRunner("runner");

        for (int i = 0; i < 3000; i++) {
            Coroutine sender = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                Address dstAddr = ctx.in();

                int j = 0;
                while (true) {
                    ctx.out(dstAddr, j);
                    cnt.suspend();
                    Validate.isTrue(j == (int) ctx.in());
                    j++;
                }
            };

            Coroutine echoer = (cnt) -> {
                Context ctx = (Context) cnt.getContext();

                while (true) {
                    Address src = ctx.source();
                    Object msg = ctx.in();
                    ctx.out(src, msg);
                    cnt.suspend();
                }
            };

            runner.addActor("echoer" + i, echoer);
            runner.addActor("sender" + i, sender, Address.fromString("runner:echoer" + +i));
        }

        latch.await();
    }

    private static void basicTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            Address dstAddr = ctx.in();

            for (int i = 0; i < 10; i++) {
                ctx.out(dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.in());
            }

            latch.countDown();
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                Address src = ctx.source();
                Object msg = ctx.in();
                ctx.out(src, msg);
                cnt.suspend();
            }
        };

        ActorRunner echoerRunner = new ActorRunner("echoer");
        ActorRunner senderRunner = new ActorRunner("sender");

        echoerRunner.addOutgoingShuttle(senderRunner.getIncomingShuttle());
        senderRunner.addOutgoingShuttle(echoerRunner.getIncomingShuttle());
        echoerRunner.addActor("echoer", echoer);
        senderRunner.addActor("sender", sender, Address.fromString("echoer:echoer"));

        latch.await();
    }
}
