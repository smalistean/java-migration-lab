package com.acme.orders.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/** Request counter. Replaces a sun.misc.Unsafe-based implementation; AtomicLong is as fast on any current JIT. */
@Component
public class RequestCounter {

    private final AtomicLong value = new AtomicLong();

    public long increment() {
        return value.incrementAndGet();
    }

    public long current() {
        return value.get();
    }
}
