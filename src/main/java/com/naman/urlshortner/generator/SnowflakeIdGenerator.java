package com.naman.urlshortner.generator;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SnowflakeIdGenerator implements IdGenerator {

    private final long epoch = 1288834974657L; // Twitter Snowflake epoch
    private final long workerIdBits = 5L; // Max 31 workers
    private final long datacenterIdBits = 5L; // Max 31 datacenters
    private final long maxWorkerId = ~(-1L << workerIdBits); // 31
    private final long maxDatacenterId = ~(-1L << datacenterIdBits); // 31
    private final long sequenceBits = 12L; // 4096 unique IDs per millisecond per node
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampShift = sequenceBits + workerIdBits + datacenterIdBits;

    private final long workerId;
    private final long datacenterId;

    private long lastTimestamp = -1L;
    private final AtomicLong sequence = new AtomicLong(0L);

    public SnowflakeIdGenerator() {
        this.workerId = getMachineWorkerId();
        this.datacenterId = getDatacenterId();

        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("Datacenter ID can't be greater than %d or less than 0", maxDatacenterId));
        }
    }

    @Override
    public long nextId() {

        long timestamp = System.currentTimeMillis();

        // Handle clock rollback
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            try {
                Thread.sleep(offset); // Wait until clock catches up
                timestamp = System.currentTimeMillis();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread interrupted while waiting for clock to catch up", e);
            }
        }

        if (lastTimestamp == timestamp) {
            long currentSequence = sequence.incrementAndGet();
            if (currentSequence >= (1 << sequenceBits)) {
                timestamp = waitUntilNextMillis(lastTimestamp);
                sequence.set(0); // Reset sequence after waiting
            }
        } else {
            sequence.set(0); // Reset sequence in a new millisecond
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << timestampShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence.getAndIncrement() & (~(-1L << sequenceBits));
    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    private long getMachineWorkerId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                return 1L; // fallback
            }
            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return 1L; // fallback
            }
            long macHash = 0;
            for (byte b : mac) {
                macHash = (macHash << 8) + (b & 0xFF);
            }
            return macHash % (maxWorkerId + 1); // Ensure it's within 0-31
        } catch (Exception e) {
            return 1L; // Fallback worker ID in case of failure
        }
    }

    private long getDatacenterId() {
        String datacenterProperty = System.getProperty("datacenter.id");
        if (datacenterProperty != null) {
            return Long.parseLong(datacenterProperty) % (maxDatacenterId + 1);
        } else {
            return (System.currentTimeMillis() & 0x1F); // Use timestamp as fallback
        }
    }

}
