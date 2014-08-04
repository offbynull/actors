package com.offbynull.peernetic.demos.chord.fsms;

public final class InitFingerTable<A> {
//    public static final String INITIAL_STATE = "start";
//    public static final String SEND_QUERY_FOR_ID = "ask_for_id";
//    public static final String AWAIT_QUERY_FOR_ID = "wait_for_id_response";
//    public static final String SEND_QUERY_FOR_CLOSEST_PREDECESSOR = "ask_for_closest_pred";
//    public static final String AWAIT_QUERY_FOR_CLOSEST_PREDECESSOR = "wait_for_closest_pred_response";
//    public static final String DONE_STATE = "done";
//    
//    private static final Duration RESEND_DURATION = Duration.ofSeconds(2);
//    private static final Duration RESEND_COUNT = Duration.ofSeconds(3);
//    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(10);
//    
//    private final FingerTable<A> fingerTable;
//    private final EndpointDirectory<A> endpointDirectory;
//    private final NonceGenerator<byte[]> nonceGenerator;
//    private final NonceManager<byte[]> nonceManager;
//    private final EndpointScheduler endpointScheduler;
//    private final Endpoint selfEndpoint;
//    
//    private final A initialAddress;
//    private final int maxIdx;
//    private int idx;
//
//    public InitFingerTable(Id selfId, A initialAddress, EndpointDirectory<A> endpointDirectory, EndpointScheduler endpointScheduler,
//            Endpoint selfEndpoint, NonceGenerator<byte[]> nonceGenerator) {
//        Validate.notNull(initialAddress);
//        Validate.notNull(selfId);
//        Validate.notNull(endpointDirectory);
//        Validate.notNull(endpointScheduler);
//        Validate.notNull(selfEndpoint);
//        Validate.notNull(nonceGenerator);
//
//        this.initialAddress = initialAddress;
//        this.fingerTable = new FingerTable<>(new InternalPointer(selfId));
//        this.endpointDirectory = endpointDirectory;
//        this.endpointScheduler = endpointScheduler;
//        this.selfEndpoint = selfEndpoint;
//        this.nonceGenerator = nonceGenerator;
//        this.nonceManager = new NonceManager<>();
//        
//        maxIdx = ChordUtils.getBitLength(selfId);
//    }
//
//    @StateHandler(INITIAL_STATE)
//    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) {
//        fsm.switchStateAndProcess(SEND_QUERY_FOR_ID, instant, unused, srcEndpoint);
//    }
//
//    @StateHandler(SEND_QUERY_FOR_ID)
//    public void handleAskForId(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) {
//        // send msg asking for id
//        Endpoint dstEndpoint = endpointDirectory.lookup(initialAddress);
//        Nonce<byte[]> nonce = nonceGenerator.generate();
//        dstEndpoint.send(selfEndpoint, new GetIdRequest(nonce.getValue()));
//        nonceManager.addNonce(instant, TIMEOUT_DURATION, nonce, null);
//        
//        // schedule sending 3 times
//        endpointScheduler.scheduleMessage(RESEND_DURATION, selfEndpoint, dstEndpoint, message);
//        
//        // request timeout
//        endpointScheduler.scheduleMessage(TIMEOUT_DURATION, selfEndpoint, selfEndpoint, new TimedOut());
//        
//        // await response
//        fsm.setState(AWAIT_QUERY_FOR_ID);
//    }
//
//    @StateHandler(AWAIT_QUERY_FOR_ID)
//    public void handleQueryResponse(String state, FiniteStateMachine fsm, Instant instant, GetIdResponse response, Endpoint srcEndpoint) {
//        Nonce<byte[]> nonce = new ByteArrayNonce(response.getNonce());
//        Optional<Object> nonceValue = nonceManager.checkNonce(nonce);
//        
//        if (nonceValue == null) {
//            // not found
//            return;
//        }
//        
//        
//        fsm.switchStateAndProcess(SEND_QUERY_FOR_CLOSEST_PREDECESSOR, instant, new Object(), srcEndpoint);
//        
//        
//    }
//
//    @StateHandler(AWAIT_QUERY_FOR_ID)
//    public void handleQueryTimeout(String state, FiniteStateMachine fsm, Instant instant, TimedOut response, Endpoint srcEndpoint) {
//        fsm.setState(DONE_STATE);
//    }
//    
//    
//    public static final class TimedOut {
//        private TimedOut() {
//            // does nothing, prevents outside instantiation
//        }
//    }
}
