package org.tdudhatra.n26.model;

import lombok.Data;

@Data
public class CompositeTransaction {
    private int count;
    private double sum;
    private double max;
    private double min;
}
