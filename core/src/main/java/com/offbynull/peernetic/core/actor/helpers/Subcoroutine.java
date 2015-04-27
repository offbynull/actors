package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;

public interface Subcoroutine<T> {
    String getSourceId();
    T run(Continuation cnt) throws Exception;
}
