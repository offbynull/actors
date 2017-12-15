
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class JdbcStoreTest {
    
    private JdbcStore fixture;
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Before
    public void before() throws SQLException {
        EmbeddedConnectionPoolDataSource ds = new EmbeddedConnectionPoolDataSource();
        ds.setDatabaseName("memory:testDB");
        ds.setCreateDatabase("create");
        ds.setLogWriter(new PrintWriter(System.out));
        
        try (Connection conn = ds.getConnection();
                Statement statement = conn.createStatement()) {
            statement.execute(
                    "CREATE TABLE SERVLET_CLIENT_IN_QUEUE (\n"
                    + "MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),\n"
                    + " ADDRESS VARCHAR(1024) NOT NULL,\n"
                    + " OFFSET INTEGER NOT NULL,\n"
                    + " DATA BLOB NOT NULL,\n"
                    + " PRIMARY KEY (MSG_NUMBER),\n"
                    + " UNIQUE (ADDRESS, OFFSET)\n"
                    + ")");
            statement.execute(
                    "CREATE TABLE SERVLET_CLIENT_IN_OFFSET (\n"
                    + " ADDRESS VARCHAR(1024) NOT NULL,\n"
                    + " OFFSET INTEGER NOT NULL,\n"
                    + " PRIMARY KEY (ADDRESS)"
                    + ")");
            statement.execute(
                    "CREATE TABLE SERVLET_CLIENT_OUT_QUEUE (\n"
                    + "MSG_NUMBER INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),\n"
                    + " ADDRESS VARCHAR(1024) NOT NULL,\n"
                    + " OFFSET INTEGER NOT NULL,\n"
                    + " DATA BLOB NOT NULL,\n"
                    + " PRIMARY KEY (MSG_NUMBER),\n"
                    + " UNIQUE (ADDRESS, OFFSET)\n"
                    + ")");
            statement.execute(
                    "CREATE TABLE SERVLET_CLIENT_OUT_OFFSET (\n"
                    + " ADDRESS VARCHAR(1024) NOT NULL,\n"
                    + " OFFSET INTEGER NOT NULL,\n"
                    + " PRIMARY KEY (ADDRESS)"
                    + ")");
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
    public void mustReturnEmptyListWhenDequeueOutOnIdThatDoesntExist() {
        List<Message> actual = fixture.dequeueOut("id", 0);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void mustAllowKeepingOffsetSameForDequeueOut() {
        List<Message> expected = Arrays.asList(
                new Message("src:id1", "http:id", "payload1"),
                new Message("src:id2", "http:id", "payload2"),
                new Message("src:id3", "http:id", "payload3")
        );        
        fixture.queueOut("id", expected);

        List<Message> actual;

        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
    }

    @Test
    public void mustAllowMovingOffsetForwardForDequeueOut() {
        List<Message> expected = Arrays.asList(
                new Message("src:id1", "http:id", "payload1"),
                new Message("src:id2", "http:id", "payload2"),
                new Message("src:id3", "http:id", "payload3")
        );        
        fixture.queueOut("id", expected);
        
        List<Message> actual;

        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
        actual = fixture.dequeueOut("id", 2);
        assertMessagesEquals(expected.subList(2, 3), actual);
        actual = fixture.dequeueOut("id", 3);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void mustFailMovingOffsetBackwardForDequeueOut() {
        List<Message> expected = Arrays.asList(
                new Message("src:id1", "http:id", "payload1"),
                new Message("src:id2", "http:id", "payload2"),
                new Message("src:id3", "http:id", "payload3")
        );        
        fixture.queueOut("id", expected);
        
        List<Message> actual;

        actual = fixture.dequeueOut("id", 0);
        assertMessagesEquals(expected, actual);
        actual = fixture.dequeueOut("id", 2);
        assertMessagesEquals(expected.subList(2, 3), actual);
        expectedException.expect(IllegalStateException.class);
        fixture.dequeueOut("id", 0);
    }








    @Test
    public void mustReturnEmptyOnInitialDequeueIn() {
        List<Message> actual = fixture.dequeueIn("id");
        assertTrue(actual.isEmpty());
    }
    
    @Test
    public void mustBulkAddOnQueueInAndReturnAllOnDequeueIn() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected);
        List<Message> actual = fixture.dequeueIn("id");

        assertMessagesEquals(expected, actual);
    }

    @Test
    public void mustIncrementallyAddOnQueueInAndReturnAllOnDequeueIn() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected.subList(0, 1));
        fixture.queueIn("id", 1, expected.subList(1, 2));
        fixture.queueIn("id", 2, expected.subList(2, 3));
        List<Message> actual = fixture.dequeueIn("id");

        assertMessagesEquals(expected, actual);
    }

    @Test
    public void mustReturnEmptyOnSubsequentDequeueIn() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected);
        List<Message> actual;
        
        actual = fixture.dequeueIn("id");
        assertMessagesEquals(expected, actual);
        
        actual = fixture.dequeueIn("id");
        assertTrue(actual.isEmpty());
    }

    @Test
    public void mustDequeueInAndRequeueIn() {
        List<Message> actual;
        List<Message> expected;
        
        
        
        expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        

        fixture.queueIn("id", 0, expected);
        
        actual = fixture.dequeueIn("id");
        assertMessagesEquals(expected, actual);

        
        
        expected = Arrays.asList(
                new Message("http:id", "dst:id4", "payload4"),
                new Message("http:id", "dst:id5", "payload5"),
                new Message("http:id", "dst:id6", "payload6")
        );        
        
        fixture.queueIn("id", 3, expected);
        
        actual = fixture.dequeueIn("id");
        assertMessagesEquals(expected, actual);
        
        
        
        actual = fixture.dequeueIn("id");
        assertTrue(actual.isEmpty());
    }
    
    @Test
    public void mustIgnoreQueueInOnExistingOffset() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        
        fixture.queueIn("id", 0, expected.subList(0, 3));
        fixture.queueIn("id", 1, expected.subList(1, 3));
        fixture.queueIn("id", 2, expected.subList(2, 3));
        List<Message> actual = fixture.dequeueIn("id");

        assertMessagesEquals(expected, actual);
    }
    
    @Test
    public void mustFailIfQueueInOnNonExistingOffset() {
        List<Message> expected = Arrays.asList(
                new Message("http:id", "dst:id1", "payload1"),
                new Message("http:id", "dst:id2", "payload2"),
                new Message("http:id", "dst:id3", "payload3")
        );        
        fixture.queueIn("id", 0, expected.subList(0, 1));
        
        expectedException.expect(IllegalStateException.class);
        fixture.queueIn("id", 2, expected);
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
