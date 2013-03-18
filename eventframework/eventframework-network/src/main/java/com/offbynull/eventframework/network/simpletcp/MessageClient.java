package com.offbynull.eventframework.network.simpletcp;

import com.offbynull.eventframework.network.message.Request;
import com.offbynull.eventframework.network.message.Response;
import com.offbynull.peernetic.eventframework.event.DefaultErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.helper.StateTracker;
import com.thoughtworks.xstream.XStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.prefixedstring.PrefixedStringCodecFactory;
import org.apache.mina.filter.compression.CompressionFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

final class MessageClient {

    private volatile IncomingEventQueue inQueue;
    private ThreadPoolExecutor threadPool;
    private StateTracker stateTracker;
    
    public MessageClient(IncomingEventQueue inQueue,
            ThreadPoolExecutor threadPool) {
        if (inQueue == null || threadPool == null) {
            throw new NullPointerException();
        }
        
        this.inQueue = inQueue;
        this.threadPool = threadPool;
        stateTracker = new StateTracker();
    }

    public void start() {
        stateTracker.start();
    }
    
    public void stop() {
        stateTracker.stop();
        inQueue = null; // avoid stuff being added to queue if stopped
    }
    
    public void sendMessage(String address, int port, Request message,
            long trackedId) {
        stateTracker.checkStarted();
        
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException();
        }
        
        if (address == null) {
            throw new NullPointerException();
        }
        
        NioSocketConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(10000L);
        connector.getFilterChain().addLast("compress",
                new CompressionFilter(CompressionFilter.COMPRESSION_MAX));
        connector.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(
                new PrefixedStringCodecFactory(Charset.forName("UTF-8"))));
        connector.getFilterChain().addLast("exec",
                new ExecutorFilter(threadPool));
        connector.setHandler(new MessageClientHandler(message, trackedId));
        connector.connect(new InetSocketAddress(address, port));
    }
    
    private final class MessageClientHandler extends IoHandlerAdapter {
        private long trackedId;
        private State state;
        private Request message;

        public MessageClientHandler(Request message, long trackedId) {
            this.trackedId = trackedId;
            this.message = message;
            state = State.WAIT_FOR_OUT;
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            Set<? extends ConstraintViolation<?>> errors = validator.validate(message);

            if (!errors.isEmpty()) {
                throw new ConstraintViolationException(errors);
            }
        
            XStream xstream = new XStream();
            String msgObj = xstream.toXML(message);
            session.write(msgObj);
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            state = State.CLOSED;
        }

        @Override
        public void messageSent(IoSession session, Object message) throws Exception {
            state = State.WAIT_FOR_IN;
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            IncomingEvent event = new DefaultErrorIncomingEvent(trackedId,
                    cause);
            
            try {
                inQueue.push(event);
            } catch (NullPointerException npe) {
                // stop has been called, force close
                session.close(true);
                return;
            }

            state = State.WAIT_FOR_CLOSE;
            session.close(true);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            if (state != State.WAIT_FOR_IN) {
                session.close(true);
                return;
            }

            String str = message.toString();
            XStream xstream = new XStream();
            Response resp = (Response) xstream.fromXML(str);

            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            Set<? extends ConstraintViolation<?>> errors = validator.validate(resp);

            if (!errors.isEmpty()) {
                throw new ConstraintViolationException(errors);
            }
            
            IncomingEvent event = new ReceiveResponseIncomingEvent(resp,
                    trackedId);
            
            try {
                inQueue.push(event);
            } catch (NullPointerException npe) {
                // stop has been called, force close
                session.close(true);
                return;
            }

            state = State.WAIT_FOR_CLOSE;
            session.close(false);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
            session.close(true);
        }
    }

    private enum State {

        WAIT_FOR_IN,
        WAIT_FOR_OUT,
        WAIT_FOR_CLOSE,
        CLOSED
    }
}
