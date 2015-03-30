package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.common.AddressUtils;
import com.offbynull.peernetic.core.Shuttle;
import com.offbynull.peernetic.core.Message;
import com.offbynull.coroutines.user.Coroutine;
import static com.offbynull.peernetic.core.common.AddressUtils.getAddress;
import com.offbynull.peernetic.core.actor.Context.BatchedOutgoingMessage;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections4.collection.UnmodifiableCollection;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ActorRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorRunnable.class);

    static final String MANAGEMENT_PREFIX = "management";
    static final String MANAGEMENT_ID = "management";
    static final String MANAGEMENT_ADDRESS = getAddress(MANAGEMENT_PREFIX, MANAGEMENT_ID);

    private final String prefix;
    private final InternalBus bus;
    private final Shuttle incomingShuttle;

    ActorRunnable(String prefix, InternalBus bus) {
        Validate.notNull(prefix);
        Validate.notNull(bus);
        Validate.notEmpty(prefix);
        
        Validate.isTrue(!MANAGEMENT_PREFIX.equals(prefix)); // management prefix is a special case

        this.prefix = prefix;
        this.bus = bus;
        incomingShuttle = new InternalShuttle(prefix, bus);
    }

    @Override
    public void run() {
        try {
            Map<String, Shuttle> outgoingShuttles = new HashMap<>(); // prefix -> shuttle
            Map<String, LoadedActor> actors = new HashMap<>(); // id -> actor

            while (true) {
                List<Message> incomingMessages = bus.pull();
                List<Message> outgoingMessages = new LinkedList<>(); // outgoing messages destined for destinations not in here

                for (Message incomingMessage : incomingMessages) {
                    Object msg = incomingMessage.getMessage();
                    String src = incomingMessage.getSourceAddress();
                    String dst = incomingMessage.getDestinationAddress();

                    if (MANAGEMENT_ADDRESS.equals(src)) {
                        processManagementMessage(msg, actors, outgoingMessages, outgoingShuttles);
                    } else {
                        processNormalMessage(msg, src, dst, actors, outgoingMessages);
                    }
                }

                sendOutgoingMessages(outgoingMessages, outgoingShuttles);
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie); // do nothing
        } finally {
            bus.close();
        }
    }

    private void processManagementMessage(Object msg, Map<String, LoadedActor> actors, List<Message> outgoingMessages,
            Map<String, Shuttle> outgoingShuttles) {
        if (msg instanceof AddActorMessage) {
            AddActorMessage aam = (AddActorMessage) msg;
            actors.put(aam.id, new LoadedActor(aam.actor, new Context()));
            List<Message> initialMessages = new LinkedList<>();
            for (Object primingMessage : aam.primingMessages) {
                String dstAddr = getAddress(prefix, aam.id);
                Message initialMessage = new Message(dstAddr, dstAddr, primingMessage);
                initialMessages.add(initialMessage);
            }
            outgoingMessages.addAll(initialMessages);
        } else if (msg instanceof RemoveActorMessage) {
            RemoveActorMessage ram = (RemoveActorMessage) msg;
            actors.remove(ram.id);
        } else if (msg instanceof AddShuttleMessage) {
            AddShuttleMessage asm = (AddShuttleMessage) msg;
            Shuttle existingShuttle = outgoingShuttles.putIfAbsent(asm.shuttle.getPrefix(), asm.shuttle);
            
            Validate.isTrue(existingShuttle == null); // unable to add a prefix for a shuttle that already exists
        } else if (msg instanceof RemoveShuttleMessage) {
            RemoveShuttleMessage rsm = (RemoveShuttleMessage) msg;
            Shuttle existingShuttle = outgoingShuttles.remove(rsm.prefix);
            
            Validate.isTrue(existingShuttle == null); // unable to remove a shuttle prefix that doesnt exist
        } else {
            LOGGER.warn("Unprocessed management message: {}", msg);
        }
    }

    private void processNormalMessage(Object msg, String src, String dst, Map<String, LoadedActor> actors, List<Message> outgoingMessages) {
        // Get actor to dump to
        String dstPrefix = AddressUtils.getPrefix(dst);
        String dstImmediateId = AddressUtils.getIdElement(dst, 0);
        Validate.isTrue(dstPrefix.equals(prefix)); // sanity check

        LoadedActor loadedActor = actors.get(dstImmediateId);
        if (loadedActor == null) {
            LOGGER.warn("Undeliverable message: id={} message={}", dstImmediateId, msg);
            return;
        }

        Actor actor = loadedActor.actor;
        Context context = loadedActor.context;
        context.setSelf(dstPrefix + SEPARATOR + dstImmediateId);
        context.setIncomingMessage(msg);
        context.setSource(src);
        context.setDestination(dst);
        context.setTime(Instant.now());

        // Pass to actor, shut down if returns false or throws exception
        boolean shutdown = false;
        try {
            if (!actor.onStep(context)) {
                shutdown = true;
            }
        } catch (Exception e) {
            shutdown = true;
        }

        if (shutdown) {
            actors.remove(dstImmediateId);
        }

        // Queue up outgoing messages
        List<BatchedOutgoingMessage> batchedOutgoingMessages = context.copyAndClearOutgoingMessages();
        for (BatchedOutgoingMessage batchedOutgoingMessage : batchedOutgoingMessages) {
            String sentFrom;
            String srcId = batchedOutgoingMessage.getSourceId();
            if (srcId == null) {
                sentFrom = dst; /* dst is the address the original message was sent to (e.g. local:actor:subaddr1:subaddr2)*/
            } else {
                sentFrom = dstPrefix + SEPARATOR + dstImmediateId + srcId;
            }
            
            Message outgoingMessage = new Message(
                    sentFrom,
                    batchedOutgoingMessage.getDestination(),
                    batchedOutgoingMessage.getMessage());

            outgoingMessages.add(outgoingMessage);
        }
    }

    private void sendOutgoingMessages(List<Message> outgoingMessages, Map<String, Shuttle> outgoingShuttles) {
        // Group outgoing messages by prefix
        Map<String, List<Message>> outgoingMap = new HashMap<>();
        for (Message outgoingMessage : outgoingMessages) {
            String outDst = outgoingMessage.getDestinationAddress();
            String outDstPrefix = AddressUtils.getPrefix(outDst);

            List<Message> batchedMessages = outgoingMap.get(outDstPrefix);
            if (batchedMessages == null) {
                batchedMessages = new LinkedList<>();
                outgoingMap.put(outDstPrefix, batchedMessages);
            }

            batchedMessages.add(outgoingMessage);
        }

        // Send outgoing messaged by prefix
        for (Entry<String, List<Message>> entry : outgoingMap.entrySet()) {
            Shuttle shuttle = outgoingShuttles.get(entry.getKey());
            Validate.isTrue(shuttle != null);
            shuttle.send(entry.getValue());
        }
    }

    String getPrefix() {
        return prefix;
    }

    Shuttle getShuttle() {
        return incomingShuttle;
    }
    
    void addActor(String id, Actor actor, Object ... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        AddActorMessage aam = new AddActorMessage(id, actor, primingMessages);
        bus.add(Collections.singletonList(new Message(MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS, aam)));
    }

    void addCoroutineActor(String id, Coroutine coroutine, Object ... primingMessages) {
        Validate.notNull(id);
        Validate.notNull(coroutine);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        AddActorMessage aam = new AddActorMessage(id, new CoroutineActor(coroutine), primingMessages);
        bus.add(Collections.singletonList(new Message(MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS, aam)));
    }

    void removeActor(String id) {
        Validate.notNull(id);
        RemoveActorMessage ram = new RemoveActorMessage(id);
        bus.add(Collections.singletonList(new Message(MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS, ram)));
    }

    void addShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        AddShuttleMessage asm = new AddShuttleMessage(shuttle);
        bus.add(Collections.singletonList(new Message(MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS, asm)));
    }

    void removeShuttle(String prefix) {
        Validate.notNull(prefix);
        RemoveShuttleMessage rsm = new RemoveShuttleMessage(prefix);
        bus.add(Collections.singletonList(new Message(MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS, rsm)));
    }
    
    private static final class LoadedActor {
        private final Actor actor;
        private final Context context;

        public LoadedActor(Actor actor, Context context) {
            Validate.notNull(actor);
            Validate.notNull(context);
            this.actor = actor;
            this.context = context;
        }
    }
    

    static final class AddActorMessage {

        private final String id;
        private final Actor actor;
        private final UnmodifiableCollection<Object> primingMessages;

        public AddActorMessage(String id, Actor actor, Object... primingMessages) {
            Validate.notNull(id);
            Validate.notNull(actor);
            Validate.notNull(primingMessages);
            Validate.noNullElements(primingMessages);

            this.id = id;
            this.actor = actor;
            this.primingMessages = (UnmodifiableCollection<Object>) UnmodifiableCollection.<Object>unmodifiableCollection(
                    Arrays.asList(primingMessages));
        }
    }

    static final class RemoveActorMessage {

        private final String id;

        public RemoveActorMessage(String id) {
            Validate.notNull(id);

            this.id = id;
        }
    }

    static final class AddShuttleMessage {

        private final Shuttle shuttle;

        public AddShuttleMessage(Shuttle shuttle) {
            Validate.notNull(shuttle);
            this.shuttle = shuttle;
        }
    }

    static final class RemoveShuttleMessage {

        private final String prefix;

        public RemoveShuttleMessage(String prefix) {
            Validate.notNull(prefix);

            this.prefix = prefix;
        }
    }
}
