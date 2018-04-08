package org.tdudhatra.n26.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tdudhatra.n26.model.Statistics;
import org.tdudhatra.n26.model.Transaction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.tdudhatra.n26.helper.Helper.assertStatistics;

public class StorageServiceTest {

    private StorageService storageService;

    @Before
    public void before() {
        storageService = new StorageService();
    }

    @Test
    public void test_saveTransaction_happyCase() {
        Instant now = Instant.now();
        boolean result1 = addTransaction(100, now);
        boolean result2 = addTransaction(200, now.minusSeconds(20));
        boolean result3 = addTransaction(300, now.minusSeconds(20));
        Assert.assertTrue(result1);
        Assert.assertTrue(result2);
        Assert.assertTrue(result3);
        Statistics statistics = storageService.getStatistics();
        assertStatistics(statistics, 600, 300, 100, 3);
    }

    @Test
    public void test_saveTransaction_amountIsZero() {
        boolean result = addTransaction(0, Instant.now());
        assertFalse(result);
    }

    @Test
    public void test_saveTransaction_timestampIsZero() {
        boolean result = addTransaction(100, Instant.EPOCH);
        assertFalse(result);
    }

    @Test
    public void test_saveTransaction_shouldAllowToStoreTxnOfAnyTimeInPast() {
        boolean result = addTransaction(1000, Instant.now().minus(10, ChronoUnit.DAYS));
        assertTrue(result);

        boolean result2 = addTransaction(2000, Instant.now().minusSeconds(20));
        assertTrue(result2);
    }

    @Test
    public void test_saveTransaction_shouldAllowToStoreTxnOfFuture() {
        boolean result = addTransaction(1000, Instant.now().plusSeconds(10));
        assertFalse(result);
    }

    @Test
    public void test_saveTransaction_minMaxIsSameWhenOneTxn() {
        boolean result = addTransaction(100, Instant.now());
        assertTrue(result);
        Statistics statistics = storageService.getStatistics();
        assertTrue(statistics.getMin() == statistics.getMax());
    }

    @Test
    public void test_saveTransaction_syncTest() throws InterruptedException {
        ExecutorService exService = Executors.newFixedThreadPool(5);
        IntStream.range(1, 1000).forEach(i -> {
            Transaction transaction = new Transaction(i, Instant.now().minusMillis(i * 2).toEpochMilli());
            exService.submit(() -> assertTrue(storageService.saveTransaction(transaction)));
        });
        exService.awaitTermination(1000, TimeUnit.MILLISECONDS);

        double expectedSum = IntStream.range(1, 1000).sum();
        double expectedMax = 999;
        double expectedMin = 1;
        int count = 999;
        Statistics statistics = storageService.getStatistics();
        assertStatistics(statistics, expectedSum, expectedMax, expectedMin, count);
    }

    @Test
    public void test_getStatistics_happyCase() {
        addTransaction(100, Instant.now());
        addTransaction(100, Instant.now().minusMillis(1000));
        addTransaction(100, Instant.now().minusSeconds(10));

        Statistics statistics = storageService.getStatistics();
        assertStatistics(statistics, 300, 100, 100, 3);
    }

    @Test
    public void test_getStatistics_onlyLastMinuteTxn() {
        addTransaction(100, Instant.now());
        addTransaction(200, Instant.now().minusMillis(1000));
        addTransaction(300, Instant.now().minusSeconds(100));
        addTransaction(400, Instant.now().minusSeconds(70));

        Statistics statistics = storageService.getStatistics();
        assertStatistics(statistics, 300, 200, 100, 2);
    }

    @Test
    public void test_getStatistics_whenNoTxn() {
        Statistics statistics = storageService.getStatistics();
        assertStatistics(statistics, 0, 0, 0, 0);
    }

    @Test
    public void test_getStatistics_whenNoTxnInLastMinute() {
        addTransaction(100, Instant.now().minusSeconds(100));
        addTransaction(100, Instant.now().minusSeconds(200));
        addTransaction(100, Instant.now().minusSeconds(300));

        Statistics statistics = storageService.getStatistics();
        assertStatistics(statistics, 0, 0, 0, 0);
    }

    @Test
    public void test_getStatistics_constantTimeComplexityCheck() {
        Instant now = Instant.now();
        IntStream.range(1, 10000000).forEach(i -> addTransaction(1, now.minusMillis(i / 1000)));

        Instant startTime = Instant.now();
        Statistics statsWithLargeInput = storageService.getStatistics();
        Instant endTime = Instant.now();
        long largeInputTimeMillis = endTime.toEpochMilli() - startTime.toEpochMilli();
        assertStatistics(statsWithLargeInput, 9999999, 1, 1, 9999999);

        storageService.clearStorage();

        Instant nowAgain = Instant.now();
        IntStream.range(1, 100).forEach(i -> addTransaction(i, nowAgain.minusMillis(i)));

        Instant startTimeSmallInput = Instant.now();
        Statistics statsWithSmallInput = storageService.getStatistics();
        Instant endTimeSmallInput = Instant.now();
        long smallInputTimeMillis = endTimeSmallInput.toEpochMilli() - startTimeSmallInput.toEpochMilli();
        assertStatistics(statsWithSmallInput, IntStream.range(1, 100).sum(), 99, 1, 99);

        // Difference of time between large input and small input will never be greater than 3 milliseconds.
        // It's always observed same time but to be on safe side gap is given of 3 millis
        assertTrue(Math.abs(largeInputTimeMillis - smallInputTimeMillis) <= 3);
    }

    @Test
    public void test_clearStorage_happyCase() {
        addTransaction(100, Instant.now());
        Statistics statistics = storageService.getStatistics();
        assertStatistics(statistics, 100, 100, 100, 1);

        storageService.clearStorage();
        Statistics newStatistics = storageService.getStatistics();
        assertStatistics(newStatistics, 0, 0, 0, 0);
    }

    @Test
    public void test_clearStorage_whenStorageIsAlreadyEmpty() {
        storageService.clearStorage();
        Statistics newStatistics = storageService.getStatistics();
        assertStatistics(newStatistics, 0, 0, 0, 0);
    }

    private boolean addTransaction(double amount, Instant time) {
        return storageService.saveTransaction(new Transaction(amount, time.toEpochMilli()));
    }

}
