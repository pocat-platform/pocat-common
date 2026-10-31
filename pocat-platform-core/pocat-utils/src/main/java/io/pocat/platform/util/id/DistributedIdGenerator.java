package io.pocat.platform.util.id;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class DistributedIdGenerator {
    private static final DistributedIdGenerator INSTANCE = new DistributedIdGenerator();

    private static final long BASE_TIMESTAMP = 1516147200000L; // 2018-01-17T09:00:00+09:00

    private static final int TIMESTAMP_BITS = 40;
    private static final int NODE_ID_BITS = 16;
    private static final int SEQUENCE_BITS = 7;

    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) -1;
    private static final long NODE_ID_MASK = (1L << NODE_ID_BITS) -1;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    private final long nodeId;

    public static DistributedIdGenerator getInstance() {
        return INSTANCE;
    }

    DistributedIdGenerator() {
        this.nodeId = generateNodeId();
    }

    public synchronized long generate() {
        long currentTimestamp = getTimestamp();
        if(currentTimestamp < this.lastTimestamp) {
            throw new IllegalStateException("Invalid system time");
        }

        if (currentTimestamp == this.lastTimestamp) {
            this.sequence = (this.sequence+1) & SEQUENCE_MASK;
            if (this.sequence == 0) {
                currentTimestamp = spinWait();
            }
        } else {
            //out of milli sec. reset sequence
            this.sequence = 0;
        }
        this.lastTimestamp = currentTimestamp;

        return currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS) | this.nodeId << SEQUENCE_BITS | this.sequence;
    }

    public long[] parse(long id) {
        long timestamp = (id >>> (NODE_ID_BITS + SEQUENCE_BITS)) + BASE_TIMESTAMP;
        long nodeId = (id >>> SEQUENCE_BITS) & NODE_ID_MASK;
        long sequence = id & SEQUENCE_MASK;

        return new long[]{timestamp, nodeId, sequence};
    }

    private long spinWait() {
        long currentTimestamp;
        do {
            Thread.onSpinWait();
            currentTimestamp = getTimestamp();
        } while (currentTimestamp == this.lastTimestamp);
        return currentTimestamp;
    }

    private long getTimestamp() {
        return System.currentTimeMillis() - BASE_TIMESTAMP;
    }

    private long generateNodeId() {
        try {
            return Arrays.hashCode(
                    InetAddress.getLocalHost().getAddress()) & 0xFFFFL;
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Failed to determine local IP address", e);
        }
    }
}