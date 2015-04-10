package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.peernetic.core.Message;
import com.offbynull.peernetic.core.common.AddressUtils;
import com.offbynull.peernetic.core.common.Serializer;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WriteRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(WriteRunnable.class);

    private final WriteBus bus;
    private final File file;
    private final String selfPrefix;
    private final Serializer serializer;

    WriteRunnable(File file, String selfPrefix, Serializer serializer) {
        Validate.notNull(file);
        Validate.notNull(selfPrefix);
        Validate.notNull(serializer);

        this.bus = new WriteBus();
        this.file = file;
        this.selfPrefix = selfPrefix;
        this.serializer = serializer;
    }

    public WriteBus getBus() {
        return bus;
    }

    @Override
    public void run() {
        LOG.info("Started writing");
        try (FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dos = new DataOutputStream(fos)) {
            while (true) {
                MessageBlock messageBlock = bus.pull();

                Instant time = messageBlock.getTime();
                UnmodifiableList<Message> messages = messageBlock.getMessages();

                List<RecordedMessage> recordedMessages = messages.stream()
                        .filter(x -> AddressUtils.isParent(selfPrefix, x.getDestinationAddress())) // only msgs that are destined for output
                        .map(x -> {
                            return new RecordedMessage(
                                    x.getSourceAddress(),
                                    AddressUtils.relativize(selfPrefix, x.getDestinationAddress()),
                                    x.getMessage());
                        })
                        .collect(Collectors.toList());
                
                RecordedBlock recordedBlock = new RecordedBlock(recordedMessages, time);

                byte[] data = serializer.serialize(recordedBlock);
                dos.writeInt(data.length);
                IOUtils.write(data, dos);
                dos.flush();
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                LOG.info("Stopping write thread (interrupted)");
            } else {
                LOG.error("Error in write thread", e);
            }
        } finally {
            bus.close(); // so stuff doesn't keep accumulating in this internalbus
        }
    }

}
