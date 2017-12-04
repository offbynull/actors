
package com.offbynull.actors.gateways.servlet.stores.jdbc;

import com.offbynull.actors.shuttle.Message;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class JdbcStoreTest {
    
    private JdbcStore fixture;
    
    @Before
    public void before() throws SQLException {
        EmbeddedConnectionPoolDataSource ds = new EmbeddedConnectionPoolDataSource();
        ds.setDatabaseName("memory:testDB");
        ds.setCreateDatabase("create");
        ds.setLogWriter(new PrintWriter(System.out));
        
        try (Connection conn = ds.getConnection();
                Statement statement = conn.createStatement()) {
            statement.execute(
                    "CREATE TABLE HTTP_CLIENT_QUEUE (\n"
                    + "  MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),\n"
                    + "  ADDRESS VARCHAR(1024) NOT NULL,\n"
                    + "  DATA BLOB NOT NULL,\n"
                    + "  PRIMARY KEY (MSG_NUMBER)\n"
                    + ")\n"
            );
        }
        
        fixture = JdbcStore.create("servlet", ds);
    }
    
    @After
    public void after() throws Exception {
        fixture.close();
        try {
            DriverManager.getConnection("jdbc:derby:memory:testDB;drop=true");
        } catch (SQLException sqle) {
            // https://db.apache.org/derby/docs/10.8/devguide/cdevdvlpinmemdb.html -- See section "Removing an in-memory database"
            if (!"08006".equals(sqle.getSQLState())) { // 08006 is expected
                throw sqle;
            }
        }
    }

    @Test
    public void mustWriteAndReadToMessageQueue() throws Exception {
        List<Message> inMsgs = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"));
        List<Message> outMsgs;
        
        fixture.write("b", inMsgs);
        outMsgs = fixture.read("b");
        
        assertMessagesEquals(inMsgs, outMsgs);
    }

    @Test
    public void mustWriteAndReadToDifferentMessageQueue() throws Exception {
        List<Message> inMsgsA = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:a:2:3:4", "payload"));
        List<Message> inMsgsB = Arrays.asList(
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"),
                new Message("actor:a:1:2:3", "servlet:b:2:3:4", "payload"));
        List<Message> outMsgsA;
        List<Message> outMsgsB;
        
        fixture.write("a", inMsgsA);
        fixture.write("b", inMsgsB);
        outMsgsA = fixture.read("a");
        outMsgsB = fixture.read("b");
        
        assertMessagesEquals(inMsgsA, outMsgsA);
        assertMessagesEquals(inMsgsB, outMsgsB);
    }

    @Test
    public void mustTakeEmptyListForNonExistantQueue() throws Exception {
        List<Message> outMsgsA = fixture.read("a");
        List<Message> outMsgsB = fixture.read("b");
        
        assertTrue(outMsgsA.isEmpty());
        assertTrue(outMsgsB.isEmpty());
    }
    
    private void assertMessagesEquals(List<Message> expected, List<Message> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Message expectedMsg = expected.get(i);
            Message actualMsg = actual.get(i);
            
            assertEquals(expectedMsg.getSourceAddress(), actualMsg.getSourceAddress());
            assertEquals(expectedMsg.getDestinationAddress(), actualMsg.getDestinationAddress());
            assertEquals(expectedMsg.getMessage(), actualMsg.getMessage());
        }
    }
    
}
