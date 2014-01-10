package com.offbynull.peernetic.overlay.chord.messages;

import com.offbynull.peernetic.overlay.common.id.Id;
import org.apache.commons.lang3.Validate;

public final class GetClosestPrecedingFinger {
    private Id id;

    public GetClosestPrecedingFinger(Id id) {
        Validate.notNull(id);
        
        this.id = id;
    }

    public Id getId() {
        return id;
    }
    
}
