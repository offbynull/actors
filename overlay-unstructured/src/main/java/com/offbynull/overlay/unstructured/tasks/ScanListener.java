package com.offbynull.overlay.unstructured.tasks;

import java.util.Set;

interface ScanListener<A> {
    void scanIterationComplete(Set<A> nodes);
}
