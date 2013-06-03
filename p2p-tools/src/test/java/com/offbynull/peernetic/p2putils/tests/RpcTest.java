package com.offbynull.peernetic.p2putils.tests;

import com.offbynull.peernetic.p2ptools.rpc.RpcSystem;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.junit.Test;
import static org.junit.Assert.*;

public class RpcTest {

    @Test
    public void testNormal() throws InterruptedException, UnknownHostException {
        RpcSystem rpcSystem = new RpcSystem();
        
        FakeItem serverItem = new FakeItem();
        rpcSystem.startTcpRpcServer(serverItem, 19995);
        
        InetSocketAddress address =
                new InetSocketAddress(InetAddress.getLocalHost(), 19995);
        FakeItem clientItem = rpcSystem.createTcpClient(FakeItem.class, address);
        
        String expected = serverItem.stringRet();
        String actual = clientItem.stringRet();
        
        assertEquals(expected, actual);
    }
}
