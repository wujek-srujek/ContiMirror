package com.jambit.conti.mirror;


public class FpsCounter {

    private int count;

    private int fps;

    private long start;

    public void inc() {
        if (System.currentTimeMillis() - start >= 1000) {
            fps = count;

            start = System.currentTimeMillis();
            count = 0;
        }

        ++count;
    }

    public int getFps() {
        return fps;
    }
}
