package com.jambit.conti.mirror;


import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class BufferQueue {

    private final ByteBuffer buffer;

    private final BlockingQueue<ByteBuffer> queue;

    public BufferQueue(ByteBuffer buffer) {
        this.buffer = buffer;
        queue = new ArrayBlockingQueue<>(1);
        queue.add(buffer);
    }

    public ByteBuffer getNonBlocking() {
        return queue.poll();
    }

    public ByteBuffer getBlocking() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void putThrowing() {
        queue.add(buffer);
    }
}
