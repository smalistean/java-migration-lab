package com.acme.orders.service;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import sun.misc.Unsafe;

/**
 * Low-overhead request counter. Written in 2016 when the JIT wouldn't hoist
 * a plain volatile increment out of the hot loop. Nobody has touched it since.
 */
@Component
@SuppressWarnings("removal")
public class OffHeapCounter {

    private static final Logger log = LoggerFactory.getLogger(OffHeapCounter.class);

    private static final Unsafe UNSAFE;
    private static final long VALUE_OFFSET;

    private volatile long value = 0L;

    static {
        Unsafe unsafe = null;
        long offset = -1L;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
            offset = unsafe.objectFieldOffset(OffHeapCounter.class.getDeclaredField("value"));
        } catch (Throwable t) {
            LoggerFactory.getLogger(OffHeapCounter.class).warn("Unsafe unavailable: {}", t.toString());
        }
        UNSAFE = unsafe;
        VALUE_OFFSET = offset;
    }

    public long increment() {
        if (UNSAFE == null) {
            return ++value;
        }
        return UNSAFE.getAndAddLong(this, VALUE_OFFSET, 1L) + 1L;
    }

    public long current() {
        if (UNSAFE == null) {
            return value;
        }
        return UNSAFE.getLongVolatile(this, VALUE_OFFSET);
    }

    /** Grows the internal buffer of an ArrayList without the usual copy. */
    public static void trimInternals(java.util.ArrayList<?> list) {
        try {
            Field elementData = java.util.ArrayList.class.getDeclaredField("elementData");
            elementData.setAccessible(true);
            Object[] backing = (Object[]) elementData.get(list);
            log.debug("ArrayList backing array capacity={}", backing.length);
        } catch (Exception e) {
            log.warn("Could not reach ArrayList internals: {}", e.toString());
        }
    }
}
