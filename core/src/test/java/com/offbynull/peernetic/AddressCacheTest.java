package com.offbynull.peernetic;

import com.offbynull.peernetic.AddressCache.RetentionMode;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class AddressCacheTest {

    @Test
    public void retainNewestAddressCacheTest() {
        AddressCache<Integer> cache = new AddressCache<>(1, 5, Collections.singleton(0), RetentionMode.RETAIN_NEWEST);

        // Add extra nodes
        cache.add(1);
        cache.add(2);
        cache.add(3);
        cache.add(4);
        
        // Try to add extra nodes again, but should not crash or do anything since it's already been added
        cache.add(1);
        cache.add(2);
        cache.add(3);
        cache.add(4);
        
        // Grab extra nodes from cache, since we asked the cache to retain newest, it should give us back the oldest entries first
        Assert.assertEquals((Integer) 0, cache.next());
        Assert.assertEquals((Integer) 1, cache.next());
        Assert.assertEquals((Integer) 2, cache.next());
        Assert.assertEquals((Integer) 3, cache.next());
        
        // Grab the last node from cache, and re-grab multiple times to make sure it doesn't get removed (because we reached our min size)
        Assert.assertEquals((Integer) 4, cache.next());
        Assert.assertEquals((Integer) 4, cache.next());
        Assert.assertEquals((Integer) 4, cache.next());
    }

    @Test
    public void retainOldestAddressCacheTest() {
        AddressCache<Integer> cache = new AddressCache<>(1, 5, Collections.singleton(0), RetentionMode.RETAIN_OLDEST);

        // Add extra nodes
        cache.add(1);
        cache.add(2);
        cache.add(3);
        cache.add(4);
        
        // Try to add extra nodes again, but should not crash or do anything since it's already been added
        cache.add(1);
        cache.add(2);
        cache.add(3);
        cache.add(4);
        
        // Grab extra nodes from cache, since we asked the cache to retain newest, it should give us back the oldest entries first
        Assert.assertEquals((Integer) 4, cache.next());
        Assert.assertEquals((Integer) 3, cache.next());
        Assert.assertEquals((Integer) 2, cache.next());
        Assert.assertEquals((Integer) 1, cache.next());
        
        // Grab the last node from cache, and re-grab multiple times to make sure it doesn't get removed (because we reached our min size)
        Assert.assertEquals((Integer) 0, cache.next());
        Assert.assertEquals((Integer) 0, cache.next());
        Assert.assertEquals((Integer) 0, cache.next());
    }
    
}
