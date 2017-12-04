package com.offbynull.actors.gateways.servlet;

import com.offbynull.actors.shuttle.Message;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ServletGatewayTest {

    private ServletGateway fixture;

    @Before
    public void setUp() {
        fixture = ServletGateway.create();
    }

    @After
    public void tearDown() {
        fixture.close();
    }

    // This is difficult to test because we need to spin up a full servlet container, but we can atleast add in a sanity test here
    @Test
    public void mustNotCrash() throws Throwable {
        HttpServlet servlet = fixture.getServlet();
        
        fixture.getIncomingShuttle().send(new Message("src:src", "servlet:test_id", "hi!"));
        
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        StringWriter out = new StringWriter();

        when(req.getMethod()).thenReturn("POST");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(
                "{\n"
                + "  id: 'test_id',\n"
                + "  messages: [\n"
                + "    {\n"
                + "      source: 'servlet:test_id',\n"
                + "      destination: 'actor:worker123:querier',\n"
                + "      type: 'java.lang.String',\n"
                + "      data: 'work'\n"
                + "    },\n"
                + "    {\n"
                + "      source: 'servlet:test_id:subsystem1',\n"
                + "      destination: 'actor:worker555',\n"
                + "      type: 'java.lang.Integer',\n"
                + "      data: 5\n"
                + "    }\n"
                + "  ]\n"
                + "}")));
        when(resp.getWriter()).thenReturn(new PrintWriter(out));

        servlet.service(req, resp);
        
        assertEquals("{\"messages\":[{\"source\":\"src:src\",\"destination\":\"servlet:test_id\",\"type\":\"java.lang.String\",\"data\":\"hi!\"}]}", out.toString());
    }

}
