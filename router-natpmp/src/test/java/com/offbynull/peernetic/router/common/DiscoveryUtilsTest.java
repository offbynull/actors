package com.offbynull.peernetic.router.common;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class DiscoveryUtilsTest {

    public DiscoveryUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Ignore // ignored because there is no way to test this without a NAT-PMP enabled router with the gateway address 192.168.1.1
    @Test
    public void findGatewayTest() throws Throwable {
        List<InetAddress> addresses = DiscoveryUtils.findGateway();

        Assert.assertEquals(Collections.singletonList(InetAddress.getByName("192.168.1.1")), addresses);
    }
}
