package com.jambit.conti.mirror;


import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends ActionBarActivity {

    public static final String IP_EXTRA = "ipExtra";

    public static final String PORT_EXTRA = "portExtra";

    private View appView;

    private TextView fpsView;

    private CCSS ccss;

    private FpsCounter fpsCounter;

    private Handler mainThreadHandler;

    private Executor networkingExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        appView = findViewById(R.id.app);
        appView.getViewTreeObserver().addOnPreDrawListener(new StartingPreDrawListener());

        mainThreadHandler = new Handler(Looper.getMainLooper());
        networkingExecutor = Executors.newSingleThreadExecutor();
    }

    // the dimensions are known in the observer right before first drawing
    // use this event to start everything
    private class StartingPreDrawListener implements ViewTreeObserver.OnPreDrawListener {

        @Override
        public boolean onPreDraw() {
            appView.getViewTreeObserver().removeOnPreDrawListener(this);

            int width = appView.getWidth();
            int height = appView.getHeight();

            fpsCounter = new FpsCounter();
            fpsView = (TextView) findViewById(R.id.fps);

            // prepare the mirror bitmap to be updated
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            ImageView mirrorView = (ImageView) findViewById(R.id.mirror);
            mirrorView.setImageDrawable(bitmapDrawable);

            // prepare the touch detector
            View touchDetector = findViewById(R.id.touch);
            touchDetector.setOnTouchListener(new TouchForwarder(width, height));

            // 4 is ARGB - the generated images will be 32bit bitmaps
            int port = getIntent().getIntExtra(PORT_EXTRA, -1);
            String ip = getIntent().getStringExtra(IP_EXTRA);
            ccss = new CCSS(width * height * 4, ip, port, new BitmapDrawableMirrorScreen(bitmapDrawable));

            return true;
        }
    }

    private class BitmapDrawableMirrorScreen implements MirrorScreen {

        private final BitmapDrawable bitmapDrawable;

        public BitmapDrawableMirrorScreen(BitmapDrawable bitmapDrawable) {
            this.bitmapDrawable = bitmapDrawable;
        }

        @Override
        public void update(final ByteBuffer buffer, final CompletionCallback completionCallback) {
            mainThreadHandler.post(new Runnable() {

                @Override
                public void run() {
                    fpsCounter.inc();
                    fpsView.setText("" + fpsCounter.getFps());

                    bitmapDrawable.getBitmap().copyPixelsFromBuffer(buffer);
                    buffer.clear();

                    completionCallback.done();

                    // force redraw of the drawable - without it, no updates will be visible
                    bitmapDrawable.invalidateSelf();
                }
            });
        }
    }

    private class TouchForwarder implements View.OnTouchListener {

        private final Rect bounds;

        public TouchForwarder(int viewWidth, int viewHeight) {
            bounds = new Rect(0, 0, viewWidth, viewHeight);
        }

        @Override
        public boolean onTouch(View v, final MotionEvent event) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            if (bounds.contains(x, y)) {
                networkingExecutor.execute(new Runnable() {

                    @Override
                    public void run() {
                        ccss.generateTouch(x, y, event.getActionMasked());
                    }
                });
            }

            return true;
        }
    }
}
