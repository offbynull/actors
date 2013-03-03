package com.offbynull.peernetic.eventframework.handler.communication;

import com.offbynull.peernetic.eventframework.handler.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.helper.StateTracker;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.prefixedstring.PrefixedStringCodecFactory;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

final class MessageServer {

    private static final String STATE_KEY = "STATE";
    private static final String PENDING_ID_KEY = "PID";
    private IoAcceptor acceptor;
    private StateTracker tracker;
    private int port;
    private long trackedId;
    private IncomingEventQueue inQueue;
    private Map<Long, IoSession> sessionByPendingId;
    private ThreadPoolExecutor threadPool;

    public MessageServer(int port, IncomingEventQueue inQueue, long trackedId,
            ThreadPoolExecutor threadPool) {
        if (inQueue == null || threadPool == null) {
            throw new NullPointerException();
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException();
        }

        tracker = new StateTracker();
        sessionByPendingId = new ConcurrentHashMap<>();
        this.port = port;
        this.inQueue = inQueue;
        this.trackedId = trackedId;
        this.threadPool = threadPool;
    }

    public void start() throws IOException {
        tracker.checkFresh();

        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec",
                new ProtocolCodecFilter(
                new PrefixedStringCodecFactory(Charset.forName("UTF-8"))));
        acceptor.getFilterChain().addLast("exec",
                new ExecutorFilter(threadPool));
        acceptor.setHandler(new MessageServerHandler());
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        acceptor.bind(new InetSocketAddress(port));
        tracker.start();
    }

    public void stop() {
        tracker.checkStarted();
        
        inQueue = null;

        acceptor.unbind();
        acceptor.dispose();
        sessionByPendingId.clear();
        tracker.stop();
    }

    public void incomingResponse(Response resp, long pendingId) {
        IoSession session = sessionByPendingId.get(pendingId);

        State state = (State) session.getAttribute(STATE_KEY);
        if (state != State.WAIT_FOR_OUT) {
            session.close(true);
            return;
        }
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
       Set<? extends ConstraintViolation<?>> errors = validator.validate(resp);

        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }

        XStream xstream = new XStream();
        String respObj = xstream.toXML(resp);
        session.write(respObj);
    }

    private final class MessageServerHandler extends IoHandlerAdapter {

        private AtomicLong pendingIdGen;

        public MessageServerHandler() {
            pendingIdGen = new AtomicLong();
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            session.setAttribute(STATE_KEY, State.CLOSED);
            long pendingId = (Long) session.getAttribute(PENDING_ID_KEY);
            sessionByPendingId.remove(pendingId);
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            long pendingId = pendingIdGen.getAndIncrement();
            session.setAttribute(STATE_KEY, State.WAIT_FOR_IN);
            session.setAttribute(PENDING_ID_KEY, pendingId);
            sessionByPendingId.put(pendingId, session);
        }

        @Override
        public void messageSent(IoSession session, Object message) throws Exception {
            session.setAttribute(STATE_KEY, State.WAIT_FOR_CLOSE);
            session.close(false);
        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            session.close(true);
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            State state = (State) session.getAttribute(STATE_KEY);
            long pendingId = (Long) session.getAttribute(PENDING_ID_KEY);

            if (state != State.WAIT_FOR_IN) {
                session.close(true);
                return;
            }

            String str = message.toString();
            XStream xstream = new XStream();
            Request req;
            
            try {
                req = (Request) xstream.fromXML(str);
            } catch (ClassCastException cce) {
                session.close(true);
                return;
            }
            
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            Set<? extends ConstraintViolation<?>> errors = validator.validate(req);

            if (!errors.isEmpty()) {
                throw new ConstraintViolationException(errors);
            }
            
            IncomingEvent event = new ReceiveMessageIncomingEvent(req,
                    session.getRemoteAddress().toString(), port, pendingId,
                    trackedId);
            
            try {
                inQueue.push(event);
            } catch (NullPointerException npe) {
                // stop has been called, force close
                session.close(true);
                return;
            }

            session.setAttribute(STATE_KEY, State.WAIT_FOR_OUT);
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
