package com.offbynull.peernetic.chord;

public final class PointerUtils {
    private PointerUtils() {
        // utility class
    }
    
    public static boolean selfPointerTest(Pointer self, Pointer other) {
        Id selfId = self.getId();
        Id otherId = other.getId();
        
        if (selfId.equals(otherId)) {
            if (!self.equals(other)) {
                throw new IllegalArgumentException();
            }
            
            return true;
        }
        
        return false;
    }
}
