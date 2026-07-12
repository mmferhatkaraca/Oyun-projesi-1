package com.orbita.flowarena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread thread;
    private MainActivity activity;

    // Game Variables
    private int width, height, centerX, centerY;
    private float coreRadius = 150f;
    private float pinLength = 120f;
    
    public int pinsLeft = 0;
    private float coreRotation = 0f;
    private float pulseRotation = 0f;
    private float speed = 1.5f;
    
    private List<Float> pinsOnCore = new ArrayList<>();
    private Float flyingPinY = null;
    private boolean isRiskPin = false;

    // State
    public boolean isPlaying = false;
    public boolean riskMode = false;
    public boolean shieldActive = false;
    
    private Paint paintCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPulse = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintChain = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        
        paintCore.setColor(Color.parseColor("#00d2d3"));
        paintCore.setStyle(Paint.Style.FILL);
        
        paintPin.setColor(Color.WHITE);
        paintPin.setStrokeWidth(8f);
        paintPin.setStrokeCap(Paint.Cap.ROUND);
        
        paintPulse.setColor(Color.parseColor("#ff9f43"));
        paintPulse.setStyle(Paint.Style.FILL);
        paintPulse.setShadowLayer(20f, 0, 0, Color.parseColor("#ff9f43"));
        setLayerType(LAYER_TYPE_SOFTWARE, paintPulse);
        
        paintChain.setColor(Color.parseColor("#4400d2d3"));
        paintChain.setStrokeWidth(4f);
    }

    public void setActivity(MainActivity act) {
        this.activity = act;
    }

    public void startLevel(int pinCount, float speed) {
        this.pinsLeft = pinCount;
        this.speed = speed;
        this.pinsOnCore.clear();
        this.coreRotation = 0f;
        this.pulseRotation = 0f;
        this.flyingPinY = null;
        this.isPlaying = true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        width = getWidth();
        height = getHeight();
        centerX = width / 2;
        centerY = height / 3;
        
        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {}
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isPlaying && flyingPinY == null && pinsLeft > 0) {
                pinsLeft--;
                activity.updatePinsUI(pinsLeft);
                flyingPinY = (float) height - 200f;
                isRiskPin = riskMode;
                vibrate(20);
            }
        }
        return true;
    }

    public void update() {
        if (!isPlaying) return;

        coreRotation = (coreRotation + speed) % 360f;
        pulseRotation = (pulseRotation + speed * 1.5f) % 360f;

        if (flyingPinY != null) {
            flyingPinY -= 60f; // Pin speed
            float targetY = centerY + coreRadius;
            
            if (flyingPinY <= targetY) {
                // Collision Logic
                float attachAngle = (360f - coreRotation) % 360f;
                float safeDist = isRiskPin ? 18f : 12f; // degrees
                
                boolean collided = false;
                for (Float a : pinsOnCore) {
                    float diff = Math.abs(a - attachAngle);
                    if (diff > 180f) diff = 360f - diff;
                    if (diff < safeDist) { collided = true; break; }
                }
                
                if (collided) {
                    if (shieldActive) {
                        shieldActive = false;
                        vibrate(100);
                    } else {
                        isPlaying = false;
                        vibrate(300);
                        activity.runOnUiThread(() -> activity.showResult(false));
                    }
                } else {
                    pinsOnCore.add(attachAngle);
                    if (pinsLeft == 0) {
                        isPlaying = false;
                        vibrate(100);
                        activity.runOnUiThread(() -> activity.showResult(true));
                    }
                }
                flyingPinY = null;
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;
        
        canvas.drawColor(Color.parseColor("#0b0c10")); // Background

        // Draw Chain
        if (pinsOnCore.size() >= 3) {
            for (int i = 0; i < pinsOnCore.size(); i++) {
                float a = (float) Math.toRadians(pinsOnCore.get(i) + coreRotation);
                float x = centerX + (float) Math.cos(a) * (coreRadius + pinLength);
                float y = centerY + (float) Math.sin(a) * (coreRadius + pinLength);
                if (i > 0) {
                    float prevA = (float) Math.toRadians(pinsOnCore.get(i-1) + coreRotation);
                    float prevX = centerX + (float) Math.cos(prevA) * (coreRadius + pinLength);
                    float prevY = centerY + (float) Math.sin(prevA) * (coreRadius + pinLength);
                    canvas.drawLine(prevX, prevY, x, y, paintChain);
                }
            }
        }

        // Draw Core
        paintCore.setColor(riskMode ? Color.parseColor("#ff4757") : Color.parseColor("#00d2d3"));
        canvas.drawCircle(centerX, centerY, coreRadius, paintCore);
        
        // Draw Shield
        if (shieldActive) {
            Paint sp = new Paint();
            sp.setColor(Color.parseColor("#331dd1a1"));
            canvas.drawCircle(centerX, centerY, coreRadius + 20f, sp);
        }

        // Draw Pulse
        float pr = (float) Math.toRadians(pulseRotation);
        float px = centerX + (float) Math.cos(pr) * coreRadius;
        float py = centerY + (float) Math.sin(pr) * coreRadius;
        canvas.drawCircle(px, py, 20f, paintPulse);

        // Draw Attached Pins
        for (Float a : pinsOnCore) {
            float angle = (float) Math.toRadians(a + coreRotation);
            float startX = centerX + (float) Math.cos(angle) * coreRadius;
            float startY = centerY + (float) Math.sin(angle) * coreRadius;
            float endX = centerX + (float) Math.cos(angle) * (coreRadius + pinLength);
            float endY = centerY + (float) Math.sin(angle) * (coreRadius + pinLength);
            canvas.drawLine(startX, startY, endX, endY, paintPin);
            canvas.drawCircle(endX, endY, 15f, paintPin);
        }

        // Draw Flying Pin
        if (flyingPinY != null) {
            paintPin.setColor(isRiskPin ? Color.parseColor("#ff4757") : Color.WHITE);
            canvas.drawLine(centerX, flyingPinY, centerX, flyingPinY + pinLength, paintPin);
            canvas.drawCircle(centerX, flyingPinY + pinLength, 15f, paintPin);
            paintPin.setColor(Color.WHITE);
        }
    }

    private void vibrate(long ms) {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        }
    }
}

class GameThread extends Thread {
    private SurfaceHolder surfaceHolder;
    private GameView gameView;
    private boolean running;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        super();
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void setRunning(boolean isRunning) {
        running = isRunning;
    }

    @Override
    public void run() {
        while (running) {
            Canvas canvas = null;
            try {
                canvas = this.surfaceHolder.lockCanvas();
                synchronized(surfaceHolder) {
                    this.gameView.update();
                    this.gameView.draw(canvas);
                }
            } catch (Exception e) {
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas); }
                    catch (Exception e) { e.printStackTrace(); }
                }
            }
            // Approx 60 FPS
            try { sleep(16); } catch (Exception e) {}
        }
    }
}
