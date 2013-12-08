package com.offbynull.overlay.unstructured;

import com.offbynull.overlay.unstructured.OverlayListener.LinkType;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

public final class IncomingLinkMaintainer<A> {
    private Timer killTimer;
    private HashMap<A, Link> addressToLinkMap;
    private OverlayListener<A> overlayListener;
    private int slotCount;
    private Lock lock;
    private State state = State.UNKNOWN;

    public IncomingLinkMaintainer(int maxJoin, OverlayListener<A> overlayListener) {
        this.slotCount = maxJoin;
        this.overlayListener = overlayListener;
        killTimer = new Timer(true);
        addressToLinkMap = new HashMap<>();
        
        lock = new ReentrantLock();
    }
    
    public void start() {
        lock.lock();
        try {
            Validate.validState(state == State.UNKNOWN);
            
            state = State.STARTED;
        } finally {
            lock.unlock();
        }
    }
    
    public void stop() {
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            state = State.STOPPED;
            
            killTimer.cancel();
        } finally {
            lock.unlock();
        }
    }
    
    public Set<A> getLinks() {
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);

            return new HashSet<>(addressToLinkMap.keySet());
        } finally {
            lock.unlock();
        }
    }

    public boolean isRoomAvailable() {
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);

            return slotCount > 0;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean createLink(A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            if (slotCount == 0 || addressToLinkMap.containsKey(address)) {
                return false;
            }
            
            slotCount--;
            
            KillTimerTask killTimerTask = new KillTimerTask(address);
            Link link = new Link(killTimerTask, address, secret);
            addressToLinkMap.put(address, link);
            killTimer.schedule(killTimerTask, 60000L);
        } finally {
            lock.unlock();
        }
        
        overlayListener.linkEstablished(address, LinkType.INCOMING);
        return true;
    }

    public void destroyLink(A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            Link link = addressToLinkMap.remove(address);
            if (link == null || !link.getSecret().equals(secret)) {
                return;
            }
            link.getTask().cancel();
        } finally {
            lock.unlock();
        }
        
        overlayListener.linkBroken(address, LinkType.INCOMING);
    }

    public boolean updateLink(A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);

        lock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            Link oldLink = addressToLinkMap.get(address);
            if (!oldLink.getSecret().equals(secret)) {
                return false;
            }
            
            oldLink.getTask().cancel();

            KillTimerTask killTimerTask = new KillTimerTask(address);
            Link newLink = new Link(killTimerTask, address, secret);

            addressToLinkMap.put(address, newLink);
            killTimer.schedule(killTimerTask, 60000L);
            
            return true;
        } finally {
            lock.unlock();
        }
    }

    private final class KillTimerTask extends TimerTask {
        private A address;
        private boolean cancelled;

        public KillTimerTask(A address) {
            this.address = address;
        }
        
        @Override
        public void run() {
            lock.lock();
            try {
                Validate.validState(state == State.STARTED);
                
                if (cancelled) {
                    return;
                }

                addressToLinkMap.remove(address);
                slotCount--;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public boolean cancel() {
            lock.lock();
            try {
                cancelled = true;
                return super.cancel();
            } finally {
                lock.unlock();
            }
        }
        
    }

    private final class Link {
        private TimerTask task;
        private A address;
        private ByteBuffer secret;

        public Link(TimerTask task, A address, ByteBuffer secret) {
            this.task = task;
            this.address = address;
            this.secret = secret;
        }

        public TimerTask getTask() {
            return task;
        }

        public A getAddress() {
            return address;
        }

        public ByteBuffer getSecret() {
            return secret;
        }
        
    }
    
    private enum State {
        UNKNOWN,
        STARTED,
        STOPPED
    }
}
