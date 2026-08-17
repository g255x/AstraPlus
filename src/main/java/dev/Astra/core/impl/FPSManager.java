/*
 * Decompiled with CFR 0.152.
 */
package dev.Astra.core.impl;

import java.util.ArrayDeque;
import java.util.Deque;

public class FPSManager {
    // timestamps are monotonic so a deque works well for O(1) pruning from head
    private final Deque<Long> records = new ArrayDeque<>();

    public void record() {
        records.addLast(System.currentTimeMillis());
    }

    public int getFps() {
        long now = System.currentTimeMillis();
        while (!records.isEmpty() && records.peekFirst() + 1000L < now) {
            records.pollFirst();
        }
        return records.size();
    }
}

