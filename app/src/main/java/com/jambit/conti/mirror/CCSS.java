package com.jambit.conti.mirror;


import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;


// I don't care in this dirty hack, but real-world code should probably make it a singleton
// also, byte imageBuffer access should most likely be exclusive - when one party is writing/reading,
// nobody else may interfere or else data corruption will occur
public class CCSS {

    private final BufferQueue bufferQueue;

    private final ByteBuffer touchEventBuffer;

    private final IntBuffer touchEventIntBuffer;

    private SocketChannel socketChannel;

    public CCSS(int capacity, final String ip, final int port, final MirrorScreen mirrorScreen) {
        ByteBuffer imageBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        this.bufferQueue = new BufferQueue(imageBuffer);
        touchEventBuffer = ByteBuffer.allocateDirect(12 /* 3 ints */).order(ByteOrder.nativeOrder());
        touchEventIntBuffer = touchEventBuffer.asIntBuffer();

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    socketChannel = SocketChannel.open();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                while (true) {
                    try {
                        socketChannel.connect(new InetSocketAddress(ip, port));
                        break;
                    } catch (IOException e) {
                        Log.e("blah", "connection failed, will retry in 1s");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            // whatever, man
                        }
                    }
                }

                try {
                    CompletionCallback completionCallback = new BufferReleaser();
                    while (true) {
                        ByteBuffer imageBuffer = bufferQueue.getBlocking();
                        // read the image
                        while (imageBuffer.hasRemaining()) {
                            socketChannel.read(imageBuffer);
                        }
                        imageBuffer.flip();

                        mirrorScreen.update(imageBuffer, completionCallback);

                        imageBuffer.clear();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public BufferQueue getBufferQueue() {
        return bufferQueue;
    }

    public void generateTouch(int x, int y, int type) {
        // someone, like the touch sensor on the mirror, generated a touch
        // send it to the server for processing
        touchEventIntBuffer.put(x);
        touchEventIntBuffer.put(y);
        touchEventIntBuffer.put(type);

        try {
            while (touchEventBuffer.hasRemaining()) {
                socketChannel.write(touchEventBuffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        touchEventBuffer.clear();
        touchEventIntBuffer.clear();
    }

    private class BufferReleaser implements CompletionCallback {

        @Override
        public void done() {
            bufferQueue.putThrowing();
        }
    }
}
