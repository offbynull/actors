package com.offbynull.peernetic.core.test;

import com.offbynull.peernetic.core.common.Serializer;
import com.offbynull.peernetic.core.gateways.recorder.RecordedBlock;
import com.offbynull.peernetic.core.gateways.recorder.RecordedMessage;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class RecordMessageSink implements MessageSink {
    private final String destinationPrefix;
    private final DataOutputStream dos;
    private final Serializer serializer;

    public RecordMessageSink(String destinationPrefix, File file, Serializer serializer) throws IOException {
        Validate.notNull(destinationPrefix);
        Validate.notNull(file);
        Validate.notNull(serializer);
        this.destinationPrefix = destinationPrefix;
        this.dos = new DataOutputStream(new FileOutputStream(file));
        this.serializer = serializer;
    }
    
    @Override
    public void writeNextMessage(String source, String destination, Instant time, Object message) throws IOException {
        if (!AddressUtils.isPrefix(destinationPrefix, destination)) {
            return;
        }
        
        RecordedMessage recordedMessage = new RecordedMessage(
                source,
                AddressUtils.relativize(destinationPrefix, destination),
                message);
        RecordedBlock recordedBlock = new RecordedBlock(Collections.singletonList(recordedMessage), time);

        byte[] data = serializer.serialize(recordedBlock);
        dos.writeBoolean(true);
        dos.writeInt(data.length);
        IOUtils.write(data, dos);
    }

    @Override
    public void close() throws Exception {
        dos.writeBoolean(false);
        IOUtils.closeQuietly(dos);
    }
    
}
