package com.offbynull.overlay.visualizer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

public final class XStreamPlayer<A> implements Player<A> {

    private DataInputStream is;
    private XStream xstream;

    public XStreamPlayer(InputStream is) {
        Validate.notNull(is);

        this.is = new DataInputStream(is);
        xstream = new XStream();
    }

    @Override
    public void play(Visualizer<A> visualizer) {
        Validate.notNull(visualizer);

        try {
            Long lastTime = null;
            while (true) {
                try {
                    switch (is.read()) {
                        case -1:
                            return;
                        case 0:
                            break;
                        default:
                            throw new IllegalStateException();
                    }

                    long time = (Long) read();
                    if (lastTime != null) {
                        Thread.sleep(time - lastTime);
                    }
                    lastTime = time;

                    String output = (String) read();
                    Command<A>[] commands = (Command<A>[]) read();

                    visualizer.step(output, commands);
                } catch (IOException | InterruptedException | XStreamException e) {
                    throw new IllegalStateException(e);
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }

    }
    
    private Object read() throws IOException {
        int len = is.readInt();
        byte[] data = new byte[len];
        is.read(data);
        
        return xstream.fromXML(new String(data, "UTF-8"));
    }

}
