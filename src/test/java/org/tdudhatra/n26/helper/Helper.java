package org.tdudhatra.n26.helper;

import org.tdudhatra.n26.model.Statistics;

import static org.junit.Assert.*;

public class Helper {

    public static void assertStatistics(Statistics statistics, double sum, double max, double min, int count) {
        assertNotNull(statistics);
        assertEquals(count, statistics.getCount());
        assertTrue(statistics.getAvg() == (count > 0 ? sum / count : 0.0));
        assertTrue(statistics.getMax() == max);
        assertTrue(statistics.getMin() == min);
        assertTrue(statistics.getSum() == sum);
    }

}
