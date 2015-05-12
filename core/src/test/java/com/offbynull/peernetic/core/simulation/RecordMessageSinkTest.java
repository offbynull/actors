package com.offbynull.peernetic.core.simulation;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.recorder.RecordedBlock;
import com.offbynull.peernetic.core.gateways.recorder.RecordedMessage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

public class RecordMessageSinkTest {
    
    private static final String DST_PREFIX = "dst";
    private Serializer serializer;
    private File file;
    private RecordMessageSink fixture;
    
    @Before
    public void setUp() throws Exception {
        serializer = new SimpleSerializer();
        file = File.createTempFile(getClass().getSimpleName(), ".messages");
        file.deleteOnExit();
        fixture = new RecordMessageSink(DST_PREFIX, file, serializer);
    }
    
    @After
    public void tearDown() throws Exception {
        fixture.close();
    }

    @Test
    public void mustWriteOutMessagesToFile() throws Exception {
        fixture.writeNextMessage("src0", DST_PREFIX, Instant.ofEpochMilli(0L), "this0");
        fixture.writeNextMessage("bad", "bad", Instant.ofEpochMilli(1L), "bad"); // should ignore because not being sent to DST_PREFIX
        fixture.writeNextMessage("src1", DST_PREFIX + ":1", Instant.ofEpochMilli(1L), "this1");
        fixture.writeNextMessage("src2", DST_PREFIX + ":2", Instant.ofEpochMilli(2L), "this2");
        fixture.close();
        
        // Read file back out
        List<RecordedMessage> recordedMessages = new ArrayList<>();
        List<Instant> recordedTimes = new ArrayList<>();
        serializer = new SimpleSerializer();
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
                recordedTimes.add(recordedBlock.getTime());
            }
        }
        
        assertEquals(3, recordedMessages.size());
        assertEquals("src0", recordedMessages.get(0).getSrcAddress());
        assertEquals("src1", recordedMessages.get(1).getSrcAddress());
        assertEquals("src2", recordedMessages.get(2).getSrcAddress());
        assertNull(recordedMessages.get(0).getDstSuffix());
        assertEquals("1", recordedMessages.get(1).getDstSuffix());
        assertEquals("2", recordedMessages.get(2).getDstSuffix());
        assertEquals("this0", recordedMessages.get(0).getMessage());
        assertEquals("this1", recordedMessages.get(1).getMessage());
        assertEquals("this2", recordedMessages.get(2).getMessage());
        
        assertEquals(3, recordedTimes.size());
        assertEquals(Instant.ofEpochMilli(0L), recordedTimes.get(0));
        assertEquals(Instant.ofEpochMilli(1L), recordedTimes.get(1));
        assertEquals(Instant.ofEpochMilli(2L), recordedTimes.get(2));
    }
    
}
