package com.offbynull.peernetic.core.test;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateways.recorder.RecordedBlock;
import com.offbynull.peernetic.core.gateways.recorder.RecordedMessage;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class ReplayMessageSource implements MessageSource {
    private final String destinationPrefix;
    private final DataInputStream dis;
    private final Serializer serializer;
    
    private Instant lastTime;
    private Iterator<RecordedMessage> msgIt;

    public ReplayMessageSource(String destinationPrefix, File file, Serializer serializer) throws IOException {
        Validate.notNull(destinationPrefix);
        Validate.notNull(file);
        Validate.notNull(serializer);
        this.destinationPrefix = destinationPrefix;
        this.dis = new DataInputStream(new FileInputStream(file));
        this.serializer = serializer;
    }
    
    @Override
    public SourceMessage readNextMessage() throws IOException {
        if (msgIt == null || !msgIt.hasNext()) {
            int size = dis.readInt();
            byte[] data = new byte[size];

            IOUtils.readFully(dis, data);
            RecordedBlock recordedBlock = (RecordedBlock) serializer.deserialize(data);
            
            lastTime = recordedBlock.getTime();
            msgIt = recordedBlock.getMessages().iterator();
            Validate.isTrue(msgIt.hasNext()); // An empty recordedBlock should never get written! There should always be something to read
                                              // at the end of this if block, because as soon as we come out of it we'll try to call .next()
        }

        RecordedMessage recordedMessage = msgIt.next();
        Validate.notNull(recordedMessage.getSrcAddress());
        Validate.notNull(recordedMessage.getDstSuffix());
        Validate.notNull(recordedMessage.getMessage());
        return new SourceMessage(
                recordedMessage.getSrcAddress(),
                AddressUtils.parentize(destinationPrefix, recordedMessage.getDstSuffix()),
                lastTime,
                recordedMessage.getMessage());
    }

    @Override
    public void close() throws Exception {
        IOUtils.closeQuietly(dis);
    }
    
}
