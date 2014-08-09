package com.offbynull.peernetic.common;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang3.Validate;

public final class DurationUtils {

    private DurationUtils() {
        // do nothing
    }

    public static Duration scheduleEarliestDuration(Duration... durations) {
        return scheduleEarliestDuration(Arrays.asList(durations));
    }

    public static Duration scheduleEarliestDuration(Collection<Duration> durations) {
        Validate.notNull(durations);
        
        Optional<Duration> minDuration = durations.stream().filter(x -> x != null).min((x, y) -> x.compareTo(y));
        
        return minDuration.orElse(null);
    }
}
