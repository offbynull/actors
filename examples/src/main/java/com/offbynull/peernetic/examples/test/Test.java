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
            ctx.allow();
            
            // First message is the priming message.
            cnt.suspend();

            // All messages after the first message are messages that we should log and echo back.
            do {
                Object msg = ctx.in();
                ctx.logDebug("Received a message: {}", msg);
                ctx.out("Echoing back " + msg);
                cnt.suspend();
            } while (true);
        };

        // Create the actor runner, logger gateway, and direct gateway.
        ActorRunner actorRunner = ActorRunner.create("actors"); // container for actors
        LogGateway logGateway = LogGateway.create(); // gateway that logs to slf4j
        DirectGateway directGateway = DirectGateway.create(); // gateway that allows allows interfacing with actors/gateways from normal java code

        // Allow the actor runner to send messages to the log gateway
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());
        
        // Allow the actor runner and the direct gateway to send messages to eachother
        actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
        directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());

        // Add the coroutine actor and prime it with a hello world message
        actorRunner.addActor("echoer", echoerActor, new Object());

        
        Scanner inScanner = new Scanner(System.in);
        while(inScanner.hasNextLine()) {
            // Read next line and forward to actor
            String input = inScanner.nextLine();
            directGateway.writeMessage("actors:echoer", input);
            
            // Wait for response from actor and print out
            String response = directGateway.readMessagePayloadOnly();
            System.out.println(response);
        }
    }

    private static void cpuTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ActorRunner runner = ActorRunner.create("runner");

        for (int i = 0; i < 3000; i++) {
            Coroutine sender = (cnt) -> {
                Context ctx = (Context) cnt.getContext();
                ctx.allow();
                
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
                ctx.allow();

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
            ctx.allow();
            
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
            ctx.allow();

            while (true) {
                Address src = ctx.source();
                Object msg = ctx.in();
                ctx.out(src, msg);
                cnt.suspend();
            }
        };

        ActorRunner echoerRunner = ActorRunner.create("echoer");
        ActorRunner senderRunner = ActorRunner.create("sender");

        echoerRunner.addOutgoingShuttle(senderRunner.getIncomingShuttle());
        senderRunner.addOutgoingShuttle(echoerRunner.getIncomingShuttle());
        echoerRunner.addActor("echoer", echoer);
        senderRunner.addActor("sender", sender, Address.fromString("echoer:echoer"));

        latch.await();
    }
}
