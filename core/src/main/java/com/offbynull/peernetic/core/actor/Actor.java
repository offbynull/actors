package com.offbynull.peernetic.core.actor;

public interface Actor {
    static final String MANAGEMENT_PREFIX = "management";
    static final String MANAGEMENT_ID = "management";
    static final String MANAGEMENT_ADDRESS = "management:management";
    
    boolean onStep(Context context) throws Exception;
}
