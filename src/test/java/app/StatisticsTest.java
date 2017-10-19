package app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class StatisticsTest {
    Statistics statistics;
    long now = Instant.now().getEpochSecond();

    @Test
    public void validBumpOperationShouldWork() {
        statistics = new Statistics(10);
        Statistics.Record record;

        record = statistics.get(now);
        assertThat(record, is(nullValue()));

        statistics.bump(now, 1);

        record = statistics.get(now);
        assertThat(record.min, equalTo(1L));
        assertThat(record.max, equalTo(1L));
        assertThat(record.sum, equalTo(1L));
        assertThat(record.count, equalTo(1L));

        statistics.bump(now, 3);

        record = statistics.get(now);
        assertThat(record.min, equalTo(1L));
        assertThat(record.max, equalTo(3L));
        assertThat(record.sum, equalTo(4L));
        assertThat(record.count, equalTo(2L));

        statistics.bump(now - 1, 4);
        record = statistics.get(now);
        assertThat(record.count, equalTo(2L));

        statistics.bump(now + 1, 2);
        record = statistics.get(now - 1);
        assertThat(record.count, equalTo(1L));

        record = statistics.fullReport();
        assertThat(record.min, equalTo(1L));
        assertThat(record.max, equalTo(4L));
        assertThat(record.sum, equalTo(10L));
        assertThat(record.count, equalTo(4L));
    }

    @Test
    public void expiresOldWhenUserRequestFullStatistics() {
        AtomicBoolean seeding = new AtomicBoolean(true);
        statistics = new Statistics((recordTs, currentTs) -> {
            if (seeding.get()) {
                return false;
            }
            return recordTs <= now - 2;
        });
        statistics.bump(now - 2, 1);
        statistics.bump(now, 3);
        statistics.bump(now - 1, 2);

        Statistics.Record record = statistics.fullReport();
        assertThat(record.min, equalTo(1L));
        assertThat(record.max, equalTo(3L));
        assertThat(record.sum, equalTo(6L));
        assertThat(record.count, equalTo(3L));
        assertThat(statistics.storage.size(), equalTo(3));

        seeding.set(false);
        record = statistics.fullReport();

        assertThat(statistics.storage.size(), equalTo(2));
        assertThat(record.min, equalTo(2L));
        assertThat(record.max, equalTo(3L));
        assertThat(record.sum, equalTo(5L));
        assertThat(record.count, equalTo(2L));
    }

    @Test
    public void expiresOldWhenUserAddNewValues() {
        AtomicBoolean seeding = new AtomicBoolean(true);
        statistics = new Statistics((recordTs, currentTs) -> {
            if (seeding.get()) {
                return false;
            }
            return recordTs <= now - 2;
        });

        statistics.bump(now - 3, 1);
        statistics.bump(now - 2, 1);
        statistics.bump(now - 1, 2);

        assertThat(statistics.storage.size(), equalTo(3));

        seeding.set(false);

        // do nothing if we just increase already existing value
        statistics.bump(now - 1, 2);
        assertThat(statistics.storage.size(), equalTo(3));

        // scan expired values when we add new one
        statistics.bump(now, 2);
        assertThat(statistics.storage.size(), equalTo(2));
    }

    @Test
    public void emptyStatsShouldReturnNull() {
        statistics = new Statistics(10);
        assertThat(statistics.storage.size(), equalTo(0));
        assertThat(statistics.fullReport().count, is(0L));
    }

    @Test
    public void shouldTakeIntoAccountBoundaryValues() {
        statistics = new Statistics(2);
        assertThat(statistics.storage.size(), equalTo(0));

        statistics.bump(now, 1);
        statistics.bump(now - 1, 2);
        statistics.bump(now - 2, 3);

        assertThat(statistics.storage.size(), is(2));
        Statistics.Record record = statistics.fullReport();
        assertThat(record.min, equalTo(1L));
        assertThat(record.max, equalTo(2L));
        assertThat(record.count, equalTo(2L));
    }

    @Test
    public void randomizedTest() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        statistics = new Statistics(60);
        AtomicLong sum = new AtomicLong(0);
        for (int batchId = 0; batchId < 1000; batchId++) {
            pool.submit(() -> {
                        for (int i = 0; i < 10000; i++) {
                            ThreadLocalRandom localRandom = ThreadLocalRandom.current();
                            int offset = localRandom.nextInt(10);
                            int val = localRandom.nextInt(100);
                            sum.addAndGet(val);
                            statistics.bump(now - offset, val);
                        }
                        statistics.fullReport();
                    }
            );
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        Statistics.Record record = statistics.fullReport();
        assertThat(record.count, equalTo(10000000L));
        assertThat(record.sum, equalTo(sum.get()));
        assertThat(statistics.storage.size(), equalTo(10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void bumpValuesFromFutureShouldFail() {
        statistics = new Statistics(60);
        statistics.bump(now + 5, 1);
    }
}
