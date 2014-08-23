package com.offbynull.peernetic.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang3.Validate;

public final class ProcessableUtils {

    private ProcessableUtils() {
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

    public static Duration invokeProcessablesAndScheduleEarliestDuration(Instant time, Processable... processables) {
        return invokeProcessablesAndScheduleEarliestDuration(time, Arrays.asList(processables));
    }

    public static Duration invokeProcessablesAndScheduleEarliestDuration(Instant time, Collection<Processable> durations) {
        Validate.notNull(durations);

        Optional<Duration> minDuration = durations.stream().map(x -> {
            Duration duration = x.process(time);
            return duration;
        }).filter(x -> x != null).min((x, y) -> x.compareTo(y));

        return minDuration.orElse(null);
    }
}
