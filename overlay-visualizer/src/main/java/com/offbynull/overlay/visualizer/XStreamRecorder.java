package com.offbynull.overlay.visualizer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class XStreamRecorder<A> implements Recorder<A> {
    private XStream xstream;
    private DataOutputStream os;

    public XStreamRecorder(OutputStream os) {
        Validate.notNull(os);
        
        this.os = new DataOutputStream(os);
        xstream = new XStream();
    }
    
    @Override
    public void recordStep(String output, Command<A>... commands) {
        try {
            os.write(0);
            write(System.currentTimeMillis());
            write(output);
            write(commands);
        } catch (IOException | XStreamException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private void write(Object o) throws IOException {
        byte[] data = xstream.toXML(o).getBytes("UTF-8");
        os.writeInt(data.length);
        os.write(data);        
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(os);
    }
    
}
