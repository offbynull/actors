package com.offbynull.actors;

import com.offbynull.actors.gateways.actor.Context;
import com.offbynull.actors.gateways.actor.stores.jdbc.JdbcStore;
import com.offbynull.coroutines.user.Coroutine;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class ActorSystemTest {

    private ActorSystem actorSystem;
    
    @Before
    public void before() throws SQLException {
        EmbeddedConnectionPoolDataSource ds = new EmbeddedConnectionPoolDataSource();
        ds.setDatabaseName("memory:testDB");
        ds.setCreateDatabase("create");
        ds.setLogWriter(new PrintWriter(System.out));
        
        try (Connection conn = ds.getConnection();
                Statement statement = conn.createStatement()) {
            statement.execute(
                    "CREATE TABLE ACTOR (\n"
                    + "    ADDRESS VARCHAR(1024) NOT NULL, \n"
                    + "    CHECKPOINT_DATA BLOB NOT NULL,\n"
                    + "    CHECKPOINT_MESSAGE_DATA BLOB NOT NULL,\n"
                    + "    CHECKPOINT_TIME BIGINT NOT NULL,\n"
                    + "    CHECKPOINT_INSTANCE INTEGER NOT NULL,\n"
                    + "    IDLE INTEGER NOT NULL,\n"
                    + "    DATA BLOB NOT NULL,\n"
                    + "    PRIMARY KEY (ADDRESS)\n"
                    + ")\n"
            );
            statement.execute(
                    "CREATE TABLE MESSAGE_QUEUE (\n"
                    + "    MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),\n"
                    + "    ADDRESS VARCHAR(1024) NOT NULL,\n"
                    + "    DATA BLOB NOT NULL,\n"
                    + "    PRIMARY KEY (MSG_NUMBER),\n"
                    + "    FOREIGN KEY (ADDRESS) REFERENCES ACTOR(ADDRESS) ON DELETE CASCADE\n"
                    + ")\n"
            );
        }
        
        JdbcStore store = JdbcStore.create("actor", ds);
        actorSystem = ActorSystem.builder()
                .withDirectGateway()
                .withActorGateway(1, store)
                .build();
    }
    
    @After
    public void after() throws Exception {
        actorSystem.close();
        try {
            DriverManager.getConnection("jdbc:derby:memory:testDB;drop=true");
        } catch (SQLException sqle) {
            // https://db.apache.org/derby/docs/10.8/devguide/cdevdvlpinmemdb.html -- See section "Removing an in-memory database"
            if (!"08006".equals(sqle.getSQLState())) { // 08006 is expected
                throw sqle;
            }
        }
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
