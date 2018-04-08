package org.tdudhatra.n26.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Statistics {
    private double sum;
    private double avg;
    private double max;
    private double min;
    private int count;
}
