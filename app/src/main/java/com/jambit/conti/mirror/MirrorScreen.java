package com.jambit.conti.mirror;


import java.nio.ByteBuffer;


public interface MirrorScreen {

    void update(ByteBuffer buffer, CompletionCallback completionCallback);
}
