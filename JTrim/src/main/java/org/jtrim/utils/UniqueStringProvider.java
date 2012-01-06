/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.utils;

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 *
 * @author User_2
 */
public final class UniqueStringProvider {
    private static final int START_INDEX = 0;
    private static final char PREFIX_CHAR = '.';
    private final ReentrantLock prefixChangeLock;
    private final AtomicInteger currentIndex;

    private volatile String currentPrefix;
    private long prefixIndex;

    public UniqueStringProvider() {
        this.prefixChangeLock = new ReentrantLock();
        this.currentIndex = new AtomicInteger(START_INDEX);
        this.currentPrefix = null;
        this.prefixIndex = START_INDEX;
    }

    private int getAndInc() {
        while (true) {
            int current = currentIndex.get();
            int next = current + 1;
            if (current == Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
            if (currentIndex.compareAndSet(current, next))
                return current;
        }
    }

    private String updatePrefix(String prefix) {
        assert prefixChangeLock.isHeldByCurrentThread();

        String result = prefix != null
                ? prefix
                : Character.toString(PREFIX_CHAR);

        prefixIndex++;
        if (prefixIndex == Long.MAX_VALUE) {
            result = PREFIX_CHAR + result;
            prefixIndex = START_INDEX;
        }

        return Long.toString(prefixIndex) + result;
    }

    public String getNewString() {
        String idPrefix;
        int newID;
        do {
            String preIDPrefix;
            // It is declared as an object (instead of string) to avoid the
            // warning because of the reference comparison.
            Object postIDPrefix;
            do {
                // currentPrefix changes rather infrequently so,
                // the second loop should succeed.
                preIDPrefix = currentPrefix;
                newID = getAndInc();
                postIDPrefix = currentPrefix;
                // Note that reference comparison is good because
                // the reference must not change at all.
            } while (preIDPrefix != postIDPrefix);
            idPrefix = preIDPrefix;

            if (newID == Integer.MAX_VALUE) {
                prefixChangeLock.lock();
                try {
                    if (currentIndex.get() == Integer.MAX_VALUE) {
                        currentPrefix = updatePrefix(currentPrefix);
                        currentIndex.set(START_INDEX);
                    }
                } finally {
                    prefixChangeLock.unlock();
                }
            }
            else {
                break;
            }
        } while (true);

        return idPrefix != null
                ? (idPrefix + Long.toString(newID))
                : Long.toString(newID);
    }
}
