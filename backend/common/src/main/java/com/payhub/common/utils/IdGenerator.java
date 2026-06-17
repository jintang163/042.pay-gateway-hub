package com.payhub.common.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IdGenerator {

    private final long workerId;

    private final long datacenterId;

    private long sequence = 0L;

    private final long twepoch = 1672531200000L;

    private final long workerIdBits = 5L;

    private final long datacenterIdBits = 5L;

    private final long maxWorkerId = ~(-1L << workerIdBits);

    private final long maxDatacenterId = ~(-1L << datacenterIdBits);

    private final long sequenceBits = 12L;

    private final long workerIdShift = sequenceBits;

    private final long datacenterIdShift = sequenceBits + workerIdBits;

    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    private final long sequenceMask = ~(-1L << sequenceBits);

    private long lastTimestamp = -1L;

    public IdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("workerId不能大于%d或小于0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenterId不能大于%d或小于0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            log.error("时钟回拨，当前时间戳{}小于上次时间戳{}", timestamp, lastTimestamp);
            throw new RuntimeException(String.format("时钟回拨，拒绝生成ID%d毫秒", lastTimestamp - timestamp));
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    private static class SingletonHolder {
        private static final IdGenerator INSTANCE = new IdGenerator(1, 1);
    }

    public static IdGenerator getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static long generateId() {
        return getInstance().nextId();
    }

    public static String generateIdStr() {
        return getInstance().nextIdStr();
    }
}
