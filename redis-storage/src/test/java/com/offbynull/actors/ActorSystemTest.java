package com.offbynull.actors;

import com.offbynull.actors.gateways.actor.Context;
import com.offbynull.actors.gateways.actor.stores.redis.RedisStore;
import com.offbynull.actors.gateways.actor.stores.redis.QueueCountController;
import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclients.test.TestConnector;
import com.offbynull.coroutines.user.Coroutine;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class ActorSystemTest {

    private ActorSystem actorSystem;
    
    @Before
    public void before() {
//        try (Jedis jedis = new Jedis("192.168.56.101", 6379)) {
//            jedis.flushDB();
//        }
//        Connector connector = new JedisPoolConnector("192.168.56.101", 6379, 1);

        Connector connector = new TestConnector();
        RedisStore store = RedisStore.create("actor", connector, new QueueCountController(1), new QueueCountController(1));

        actorSystem = ActorSystem.builder()
                .withDirectGateway()
                .withActorGateway(1, store)
                .build();
    }
    
    @After
    public void after() throws Exception {
        actorSystem.close();
        actorSystem.join();
    }

    @Test(timeout = 5000L)
    public void mustCreateAndCommunicateActorsAndGateways() throws Exception {
        Coroutine echoer = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();

            // tell test that echoer is ready
            ctx.out("direct:test", "echoer_ready");
            cnt.suspend();

            ctx.out("actor:sender", ctx.in());
        };

        Coroutine sender = cnt -> {
            Context ctx = (Context) cnt.getContext();
            ctx.allow();
            
            // tell test that sender is ready
            ctx.out("direct:test", "sender_ready");
            cnt.suspend();
            
            // next message should be "go", telling use to send a message to the echoer, wait for its response, and forward it back to the
            // direct gateway
            ctx.out("actor:echoer", "echo_msg"); // send msg to get echoer
            cnt.suspend();
            String response = ctx.in();          // get msg from echoer and forward it to direct gateway
            ctx.out("direct:test", response);
        };


        actorSystem.getDirectGateway().listen("direct:test");
        Object directMsg;

        // start echoer
        actorSystem.getActorGateway().addActor("echoer", echoer, new Object());
        directMsg = actorSystem.getDirectGateway().readMessagePayloadOnly("direct:test");
        assertEquals("echoer_ready", directMsg);

        // start sender
        actorSystem.getActorGateway().addActor("sender", sender, new Object());
        directMsg = actorSystem.getDirectGateway().readMessagePayloadOnly("direct:test");
        assertEquals("sender_ready", directMsg);

        // tell sender to send
        actorSystem.getDirectGateway().writeMessage("actor:sender", "go");
        directMsg = actorSystem.getDirectGateway().readMessagePayloadOnly("direct:test");
        assertEquals("echo_msg", directMsg);
    }
    
}
