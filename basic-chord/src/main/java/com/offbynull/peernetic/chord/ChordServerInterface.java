package com.offbynull.peernetic.chord;

import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import java.util.List;

/**
 * An interface for other clients to pass in to their RPC mechanisms so they can
 * have access to us.
 * @author Kasra Faghihi
 */
public interface ChordServerInterface {
    void notify(BitLimitedPointer predecessor);
    BitLimitedPointer getPredecessor();
    BitLimitedId getId();
    RouteResult route(BitLimitedId id);
    List<BitLimitedPointer> getSuccesorList();
    long ping();
}
