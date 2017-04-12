
package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.cache.Cacher;
import com.offbynull.actors.core.cache.FileSystemCacher;
import com.offbynull.actors.core.context.Context;
import static com.offbynull.actors.core.context.Context.SuspendFlag.RELEASE;
import com.offbynull.actors.core.context.ObjectStreamSerializer;
import com.offbynull.actors.core.gateways.direct.DirectGateway;
import com.offbynull.coroutines.user.Coroutine;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class ActorCacheTest {
    
    public Path tempPath;
    
    @Before
    public void before() throws Exception {
        tempPath = Files.createTempDirectory(ActorCacheTest.class.getSimpleName());
    }
    
    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(tempPath.toFile());
    }
    
    @Test(timeout = 2000L)
    public void mustCacheAndRestoreState() throws Exception {
        try (Cacher cacher = FileSystemCacher.create(new ObjectStreamSerializer(), tempPath);
                ActorRunner runner = ActorRunner.create("runner", 1, cacher);
                DirectGateway direct = DirectGateway.create("direct");){
            
            runner.addOutgoingShuttle(direct.getIncomingShuttle());
            direct.addOutgoingShuttle(runner.getIncomingShuttle());
            
            Coroutine actor0 = (Serializable & Coroutine) cnt -> {
                Context ctx = (Context) cnt.getContext();
                ctx.allow();
                ctx.out("direct", "ready");

                int counter = 10;
                while (true) {
                    cnt.suspend();
                    
                    String msg = ctx.in();
                    ctx.out("echo " + counter + ":" + msg);
                    counter++;
                    
                    ctx.cache(true);
                    ctx.mode(RELEASE);
                }
            };

            runner.addActor("actor0", actor0, new Object());
            assertEquals("ready", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hi");
            assertEquals("echo 10:hi", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hello");
            assertEquals("echo 11:hello", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "bonjour");
            assertEquals("echo 12:bonjour", direct.readMessagePayloadOnly());
        }
    }
    
    @Test(timeout = 2000L)
    public void mustRestoreStateAfterFullRestart() throws Exception {
        Coroutine actor0 = (Serializable & Coroutine) cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("direct", "ready");

            int counter = 0;
            while (true) {
                cnt.suspend();

                String msg = ctx.in();
                ctx.out("echo " + counter + ":" + msg);
                counter++;

                ctx.cache(true);
                ctx.mode(RELEASE);
            }
        };
            
            
        try (Cacher cacher = FileSystemCacher.create(new ObjectStreamSerializer(), tempPath);
                ActorRunner runner = ActorRunner.create("runner", 1, cacher);
                DirectGateway direct = DirectGateway.create("direct");){
            
            runner.addOutgoingShuttle(direct.getIncomingShuttle());
            direct.addOutgoingShuttle(runner.getIncomingShuttle());

            
            runner.addActor("actor0", actor0, new Object());
            
            
            assertEquals("ready", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hi");
            assertEquals("echo 0:hi", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hello");
            assertEquals("echo 1:hello", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "bonjour");
            assertEquals("echo 2:bonjour", direct.readMessagePayloadOnly());
        }


        try (Cacher cacher = FileSystemCacher.create(new ObjectStreamSerializer(), tempPath);
                ActorRunner runner = ActorRunner.create("runner", 1, cacher);
                DirectGateway direct = DirectGateway.create("direct");){
            
            runner.addOutgoingShuttle(direct.getIncomingShuttle());
            direct.addOutgoingShuttle(runner.getIncomingShuttle());
            
            
            direct.writeMessage("runner:actor0", "hola");
            assertEquals("echo 3:hola", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "salaam");
            assertEquals("echo 4:salaam", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "namaste");
            assertEquals("echo 5:namaste", direct.readMessagePayloadOnly());
        }
    }

    @Test(timeout = 4000L)
    public void mustDropStateAfterActorTermination() throws Exception {
        Coroutine actor0 = (Serializable & Coroutine) cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("direct", "ready");

            int counter = 0;
            while (counter < 2) {
                cnt.suspend();

                String msg = ctx.in();
                ctx.out("echo " + counter + ":" + msg);
                counter++;
                
                ctx.cache(true);
                ctx.mode(RELEASE);
            }
        };
            
            
        try (Cacher cacher = FileSystemCacher.create(new ObjectStreamSerializer(), tempPath);
                ActorRunner runner = ActorRunner.create("runner", 1, cacher);
                DirectGateway direct = DirectGateway.create("direct");){
            
            runner.addOutgoingShuttle(direct.getIncomingShuttle());
            direct.addOutgoingShuttle(runner.getIncomingShuttle());

            
            runner.addActor("actor0", actor0, new Object());
            
            
            assertEquals("ready", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hi");
            assertEquals("echo 0:hi", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hello");
            assertEquals("echo 1:hello", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "bonjour");
            assertNull(direct.readMessagePayloadOnly(250L, TimeUnit.MILLISECONDS));
        }
        
        
        try (Cacher cacher = FileSystemCacher.create(new ObjectStreamSerializer(), tempPath);
                ActorRunner runner = ActorRunner.create("runner", 1, cacher);
                DirectGateway direct = DirectGateway.create("direct");){
            
            runner.addOutgoingShuttle(direct.getIncomingShuttle());
            direct.addOutgoingShuttle(runner.getIncomingShuttle());
            
            
            direct.writeMessage("runner:actor0", "hola");
            assertNull(direct.readMessagePayloadOnly(250L, TimeUnit.MILLISECONDS));
            
            direct.writeMessage("runner:actor0", "salaam");
            assertNull(direct.readMessagePayloadOnly(250L, TimeUnit.MILLISECONDS));
            
            direct.writeMessage("runner:actor0", "namaste");
            assertNull(direct.readMessagePayloadOnly(250L, TimeUnit.MILLISECONDS));
        }
    }
    
    @Test(timeout = 4000L)
    public void mustRestoreActiveActorOnRestart() throws Exception {
        Coroutine actor0 = (Serializable & Coroutine) cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            ctx.out("direct", "ready");
            
            String msg;
            ctx.cache(true);
            ctx.mode(RELEASE);
            
            
            cnt.suspend();
            msg = ctx.in();
            ctx.out("echo 0:" + msg);


            cnt.suspend();
            msg = ctx.in();
            ctx.out("echo 1:" + msg);


            cnt.suspend();
            msg = ctx.in();
            ctx.out("echo 2:" + msg);
        };
            
            
        try (Cacher cacher = FileSystemCacher.create(new ObjectStreamSerializer(), tempPath);
                ActorRunner runner = ActorRunner.create("runner", 1, cacher);
                DirectGateway direct = DirectGateway.create("direct");){
            
            runner.addOutgoingShuttle(direct.getIncomingShuttle());
            direct.addOutgoingShuttle(runner.getIncomingShuttle());

            
            runner.addActor("actor0", actor0, new Object());
            
            
            assertEquals("ready", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hi");
            assertEquals("echo 0:hi", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hello");
            assertEquals("echo 1:hello", direct.readMessagePayloadOnly());
        }
        
        
        try (Cacher cacher = FileSystemCacher.create(new ObjectStreamSerializer(), tempPath);
                ActorRunner runner = ActorRunner.create("runner", 1, cacher);
                DirectGateway direct = DirectGateway.create("direct");){
            
            runner.addOutgoingShuttle(direct.getIncomingShuttle());
            direct.addOutgoingShuttle(runner.getIncomingShuttle());
            
            
            direct.writeMessage("runner:actor0", "hi");
            assertEquals("echo 0:hi", direct.readMessagePayloadOnly());
            
            direct.writeMessage("runner:actor0", "hello");
            assertEquals("echo 1:hello", direct.readMessagePayloadOnly());
        }
    }
}
