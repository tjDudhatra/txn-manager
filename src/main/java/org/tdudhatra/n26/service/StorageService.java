package org.tdudhatra.n26.service;

import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.tdudhatra.n26.model.CompositeTransaction;
import org.tdudhatra.n26.model.Statistics;
import org.tdudhatra.n26.model.Transaction;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Service
public class StorageService {

    /**
     * Main Storage.
     * Where values are:
     * key = timestamp in seconds
     * value = instance of {@link CompositeTransaction} which stores compressed info of all transaction occurred during
     * this second. Keys will be sorted in descending order.
     */
    private static TreeMap<Long, CompositeTransaction> storage;

    public StorageService() {
        storage = new TreeMap<>(Comparator.reverseOrder());
    }

    public synchronized boolean saveTransaction(@NonNull Transaction transaction) {
        double amount = transaction.getAmount();
        long timestamp = transaction.getTimestamp();

        // I am not sure whether this condition is needed or not but for me it make sense.
        if (amount == 0 || timestamp <= 0 || Instant.ofEpochMilli(timestamp).isAfter(Instant.now())) {
            return false;
        }
        long timestampInSeconds = timestamp / 1000;
        if (storage.get(timestampInSeconds) == null) {
            CompositeTransaction compositeTransaction = new CompositeTransaction();
            compositeTransaction.setMin(amount);
            storage.put(timestampInSeconds, compositeTransaction);
        }
        CompositeTransaction compositeTransaction = storage.get(timestampInSeconds);
        compositeTransaction.setCount(compositeTransaction.getCount() + 1);
        compositeTransaction.setSum(compositeTransaction.getSum() + amount);
        if (amount > compositeTransaction.getMax()) {
            compositeTransaction.setMax(amount);
        } else if (amount < compositeTransaction.getMin()) {
            compositeTransaction.setMin(amount);
        }
        return true;
    }

    /**
     * This function will always return statistics of last 60 seconds.
     *
     * @return {@link Statistics}
     */
    public Statistics getStatistics() {
        int count = 0;
        double sum = 0;
        double max = 0;
        double min = 0;
        long last60Seconds = Instant.now().minusSeconds(60).getEpochSecond();

        for (Map.Entry<Long, CompositeTransaction> entry : storage.entrySet()) {
            long txnTimestamp = entry.getKey();
            CompositeTransaction transaction = entry.getValue();

            // This condition makes this method's complexity O(1) because this loop will never execute more than 60
            // times.
            if (txnTimestamp < last60Seconds) {
                break;
            }
            count = count + transaction.getCount();
            sum = sum + transaction.getSum();
            if (transaction.getMax() > max) {
                max = transaction.getMax();
            }
            if (transaction.getMin() < min || min == 0) {
                min = transaction.getMin();
            }
        }
        return new Statistics(sum, (count > 0 ? sum / count : 0), max, min, count);
    }

    public void clearStorage() {
        if (storage.isEmpty()) {
            return;
        }
        storage.clear();
    }
}
