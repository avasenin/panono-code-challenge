package app;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Storage to store and aggregate statistical data.
 * <p>
 * Storage expires old values passively. There are several cases when storage
 * tries to clean up garbage:
 * <ul>
 * <li>Storage discovers that user requested an expired key. This key will be removed from storage</li>
 * <li>Expired keys are discovered when full report is generated. All expired keys will be removed from storage</li>
 * <li>New timestamp is added to the storage. Storage scans for all expired values</li>
 * </ul>
 * <p>
 * The number of elements in storage <= ttl all the time.
 * <p>
 * All operations are thread safe.
 */
public class Statistics {
    final ConcurrentMap<Long, AtomicReference<Record>> storage;
    final BiPredicate<Long, Long> expiredPred;

    /**
     * Creates new storage for statistics with fixed time window.
     *
     * @param ttl - window time
     */
    public Statistics(int ttl) {
        this((ts, now) -> (ttl <= now - ts));
    }

    /**
     * Creates new storage for statistics with custom expiration predicate.
     * <p>
     * Predicate accept timestamp of a storage record and current time as first and second
     * argument respectively. It returns true if record is expired.
     *
     * @param pred - expiration predicate
     */
    public Statistics(BiPredicate<Long, Long> pred) {
        this.expiredPred = pred;
        this.storage = new ConcurrentHashMap<>();
    }

    /**
     * Bumps the given value for a specified timestamp.
     *
     * @param timestamp epoch time in UTC
     * @param count     the value to bump
     * @throws ArithmeticException if the result overflows long
     */
    public void bump(long timestamp, long count) {
        long now = epoch();

        if (now + 1 < timestamp) {
            throw new IllegalArgumentException(
                    String.format("Bump for a timestamp from future %d (now: %d)", timestamp, now));
        }

        if (expiredPred.test(timestamp, now)) {
            return;
        }


        AtomicReference<Record> newRecord = new AtomicReference<>(new Record(count));
        AtomicReference<Record> record = storage.putIfAbsent(timestamp, newRecord);

        if (record != null) {
            record.updateAndGet((x) -> x.bump(count));
        } else {
            // new record is added to storage. try to clean up garbage
            removeOldValue(now);
        }
    }

    /**
     * Returns the statistics for a specified timestamp.
     * <p>
     * If value has empty statistics then null is returned.
     *
     * @param timestamp epoch time in UTC
     * @return statistics for a specified timestamp.
     */
    public Record get(long timestamp) {
        if (expiredPred.test(timestamp, epoch())) {
            storage.remove(timestamp);
            return null;
        }

        AtomicReference<Record> ref = storage.get(timestamp);
        if (null == ref) {
            return null;
        }
        return ref.get();
    }

    /**
     * Returns the statistics for all not expired records.
     * <p>
     * If value has empty statistics then null is returned.
     *
     * @return statistics for a all not expired records.
     */
    public Record fullReport() {
        return reduce(Record::merge);
    }

    Record reduce(BiFunction<Record, Record, Record> reducer) {
        long now = epoch();
        Record acc = null;
        for (Map.Entry<Long, AtomicReference<Record>> entry : storage.entrySet()) {
            if (!expiredPred.test(entry.getKey(), now)) {
                Record r = entry.getValue().get();
                acc = reducer.apply(acc, r);
            } else {
                storage.remove(entry.getKey());
            }
        }
        return acc;
    }

    private long epoch() {
        return Instant.now().getEpochSecond();
    }

    private void removeOldValue(long now) {
        Set<Long> set = storage.keySet();
        for (long ts : set) {
            if (expiredPred.test(ts, now)) {
                set.remove(ts);
            }
        }
    }

    public static class Record {
        final long min;
        final long max;
        final long sum;
        final long count;

        Record(long val) {
            this(val, val, val, 1);
        }

        Record(long min, long max, long sum, long count) {
            this.min = min;
            this.max = max;
            this.sum = sum;
            this.count = count;
        }

        Record bump(long val) {
            return merge(this, new Record(val));
        }

        static Record merge(Record a, Record r) {
            if (null == a) {
                return r;
            }
            if (null == r) {
                return null;
            }
            return new Record(
                    Math.min(a.min, r.min),
                    Math.max(a.max, r.max),
                    Math.addExact(a.sum, r.sum),
                    Math.addExact(a.count, r.count)
            );
        }
    }
}

