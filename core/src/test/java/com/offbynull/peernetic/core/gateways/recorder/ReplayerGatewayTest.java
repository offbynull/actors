package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttles.test.NullShuttle;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ReplayerGatewayTest {

    @Test
    public void mustReplayMessages() throws Exception {
        File file = File.createTempFile(getClass().getSimpleName(), "data");

        Serializer serializer = new SimpleSerializer();
        try (FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dos = new DataOutputStream(fos)) {

            for (int i = 0; i < 10; i++) {
                Instant time = Instant.now();
                List<RecordedMessage> recordedMessages = Collections.singletonList(
                        new RecordedMessage(Address.of("fake"), Address.of(), i));

                RecordedBlock recordedBlock = new RecordedBlock(recordedMessages, time);

                byte[] data = serializer.serialize(recordedBlock);
                dos.writeBoolean(true);
                dos.writeInt(data.length);
                IOUtils.write(data, dos);
                dos.flush();
            }

            dos.writeBoolean(false);
            dos.flush();
        }

        List<Integer> msgs = Collections.synchronizedList(new ArrayList<Integer>());
        CountDownLatch latch = new CountDownLatch(1);
        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            try {
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
                cnt.suspend();
                msgs.add((Integer) ctx.getIncomingMessage());
            } finally {
                latch.countDown();
            }
        };

        ActorThread echoerThread = ActorThread.create("echoer");

        // Wire echoer to send back to null
        echoerThread.addOutgoingShuttle(new NullShuttle("sender"));

        // Add coroutines
        echoerThread.addCoroutineActor("echoer", echoer);

        // Create replayer that mocks out sender and replays previous events to echoer
        ReplayerGateway replayerGateway = ReplayerGateway.replay(
                echoerThread.getIncomingShuttle(),
                Address.of("echoer", "echoer"),
                file,
                new SimpleSerializer());
        replayerGateway.await();

        latch.await();
        echoerThread.close();
        echoerThread.join();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), msgs);

    }

}
