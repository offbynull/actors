package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RecorderGatewayTest {

    @Test
    public void mustRecordMessages() throws Exception {
        File file = File.createTempFile(getClass().getSimpleName(), "data");

        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        // Create actor threads
        ActorThread echoerThread = ActorThread.create("echoer");
        ActorThread senderThread = ActorThread.create("sender");

        // Create recorder that records events coming to echoer and then passes it along to echoer
        RecorderGateway echoRecorderGateway = RecorderGateway.record(
                "recorder",
                echoerThread.getIncomingShuttle(),
                "echoer:echoer",
                file,
                new SimpleSerializer());
        Shuttle echoRecorderShuttle = echoRecorderGateway.getIncomingShuttle();

        // Wire sender to send to echoerRecorder instead of echoer
        senderThread.addOutgoingShuttle(echoRecorderShuttle);

        // Wire echoer to send back directly to recorder
        echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());

        // Add coroutines
        echoerThread.addCoroutineActor("echoer", echoer);
        senderThread.addCoroutineActor("sender", sender, "recorder");

        latch.await();
        echoRecorderGateway.close();
        echoRecorderGateway.await();
        
        
        
        // Read file back out
        List<RecordedMessage> recordedMessages = new ArrayList<>();
        Serializer serializer = new SimpleSerializer();
        try (FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(fis)) {
            while (true) {
                boolean hasMore = dis.readBoolean();
                if (!hasMore) {
                    break;
                }

                int size = dis.readInt();
                byte[] data = new byte[size];
                
                IOUtils.readFully(dis, data);
                RecordedBlock recordedBlock = (RecordedBlock) serializer.deserialize(data);
                recordedMessages.addAll(recordedBlock.getMessages());
            }
        }
        
        assertEquals(10, recordedMessages.size());
    }

}
