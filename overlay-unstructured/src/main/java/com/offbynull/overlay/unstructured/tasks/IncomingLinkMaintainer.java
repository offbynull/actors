package com.offbynull.overlay.unstructured.tasks;

import com.offbynull.overlay.unstructured.tasks.OverlayListener.LinkType;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class IncomingLinkMaintainer<A> {
    private Timer killTimer;
    private HashMap<A, Link> addressToLinkMap;
    private OverlayListener<A> overlayListener;
    private int slotCount;
    private Lock lock;

    public IncomingLinkMaintainer(int maxJoin, OverlayListener<A> overlayListener) {
        this.slotCount = maxJoin;
        this.overlayListener = overlayListener;
        killTimer = new Timer(true);
        addressToLinkMap = new HashMap<>();
        
        lock = new ReentrantLock();
    }
    
    public void start() {
    }
    
    public void stop() {
        killTimer.cancel();
    }
    
    public boolean createLink(A address, ByteBuffer secret) {
        lock.lock();
        try {
            if (slotCount == 0) {
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
        lock.lock();
        try {
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

    public void updateLink(A address, ByteBuffer secret) {
        lock.lock();
        try {
            Link oldLink = addressToLinkMap.get(address);
            if (!oldLink.getSecret().equals(secret)) {
                return;
            }
            
            oldLink.getTask().cancel();

            KillTimerTask killTimerTask = new KillTimerTask(address);
            Link newLink = new Link(killTimerTask, address, secret);

            addressToLinkMap.put(address, newLink);
            killTimer.schedule(killTimerTask, 60000L);
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
}
