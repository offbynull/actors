package com.offbynull.peernetic.overlay.unstructured;

import java.util.Set;

interface ScanListener<A> {
    void scanIterationComplete(Set<A> nodes);
}
