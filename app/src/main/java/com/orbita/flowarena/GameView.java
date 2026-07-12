package com.orbita.flowarena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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
    private GameThread thread = null;
    private MainActivity activity;

    private int width, height, centerX, centerY;
    private float coreRadius = 140f;
    private float pinLength = 180f;
    
    public int pinsLeft = 0;
    private float coreRotation = 0f;
    private float pulseRotation = 0f;
    private float currentSpeed = 0f;
    private LevelData currentLevelData;
    
    private List<Float> pinsOnCore = new ArrayList<>();
    private Float flyingPinY = null;
    private boolean isRiskPin = false;

    private boolean isPlaying = false;
    private boolean riskMode = false;
    private boolean shieldActive = false;
    
    private int frameCount = 0;
    private int combo = 0;
    
    // Paints for Neon Aesthetics
    private Paint paintCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPinLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPinHead = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPulse = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintChain = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintShield = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        
        // Neon Turquoise Core
        paintCore.setStyle(Paint.Style.FILL);
        paintCore.setColor(Color.parseColor("#00D2D3"));
        paintCore.setShadowLayer(40f, 0, 0, Color.parseColor("#00D2D3"));
        
        // White Pins
        paintPinLine.setColor(Color.WHITE);
        paintPinLine.setStrokeWidth(8f);
        paintPinLine.setStrokeCap(Paint.Cap.ROUND);
        paintPinHead.setStyle(Paint.Style.FILL);
        
        // Amber Pulse Target
        paintPulse.setColor(Color.parseColor("#FF9F43"));
        paintPulse.setStyle(Paint.Style.FILL);
        paintPulse.setShadowLayer(30f, 0, 0, Color.parseColor("#FF9F43"));
        
        // Chain Geometry (Anatolian Sci-Fi)
        paintChain.setColor(Color.parseColor("#8800D2D3"));
        paintChain.setStrokeWidth(4f);
        
        // Shield
        paintShield.setColor(Color.parseColor("#441DD1A1"));
        paintShield.setStyle(Paint.Style.FILL);
        paintShield.setShadowLayer(20f, 0, 0, Color.parseColor("#1DD1A1"));

        // Software layer required for extensive ShadowLayers in Android Canvas
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setActivity(MainActivity act) {
        this.activity = act;
    }

    public void startLevel(LevelData data) {
        this.currentLevelData = data;
        this.pinsLeft = data.targetPins;
        this.currentSpeed = data.baseSpeed;
        this.pinsOnCore.clear();
        this.coreRotation = 0f;
        this.pulseRotation = 0f;
        this.flyingPinY = null;
        this.frameCount = 0;
        this.combo = 0;
        this.shieldActive = false;
        this.riskMode = false;
        this.isPlaying = true;
    }
    
    public void toggleRiskMode() {
        this.riskMode = !this.riskMode;
    }
    
    public boolean isRiskMode() { return riskMode; }
    
    public void activateShield() {
        this.shieldActive = true;
    }
    
    public boolean isShieldActive() { return shieldActive; }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        width = getWidth();
        height = getHeight();
        centerX = width / 2;
        centerY = (int)(height * 0.35); // Core positioned in the upper middle
        
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
                flyingPinY = (float) height - 200f; // Start position of thrown pin
                isRiskPin = riskMode;
                vibrate(15);
            }
        }
        return true;
    }

    public void update() {
        if (!isPlaying || currentLevelData == null) return;
        frameCount++;

        // Reverse Direction Mechanic
        if (currentLevelData.reverseFrameInterval > 0 && frameCount % currentLevelData.reverseFrameInterval == 0) {
            currentSpeed *= -1;
            activity.showToast("DÖNÜŞ YÖNÜ DEĞİŞTİ!");
        }

        // Rotations
        coreRotation = (coreRotation + currentSpeed) % 360f;
        pulseRotation = (pulseRotation + currentLevelData.pulseSpeed) % 360f;

        // Pin Flying Logic
        if (flyingPinY != null) {
            flyingPinY -= 90f; // Extremely fast, crisp travel speed
            float targetY = centerY + coreRadius;
            
            if (flyingPinY <= targetY) {
                // Attach Logic
                float attachAngle = (360f - coreRotation) % 360f;
                float safeDist = isRiskPin ? 18f : 12f; // Risk mode requires precision
                
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
                        activity.showToast("KALKAN KIRILDI!");
                    } else {
                        isPlaying = false;
                        vibrate(300);
                        activity.onLevelEnded(false);
                    }
                } else {
                    // Check Pulse Window (Perfect hit)
                    if (currentLevelData.pulseSpeed > 0) {
                        float pulseAngleReal = (360f - pulseRotation) % 360f;
                        float pulseDiff = Math.abs(attachAngle - pulseAngleReal);
                        if (pulseDiff > 180f) pulseDiff = 360f - pulseDiff;
                        
                        if (pulseDiff < 20f) {
                            combo++;
                            activity.showToast("PERFECT x" + combo);
                            vibrate(50);
                        } else {
                            combo = 0;
                            vibrate(15);
                        }
                    } else {
                        vibrate(15);
                    }
                    
                    pinsOnCore.add(attachAngle);
                    if (pinsLeft <= 0) {
                        isPlaying = false;
                        vibrate(150);
                        activity.onLevelEnded(true);
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
        
        // Deep Dark Background
        canvas.drawColor(Color.parseColor("#0B0C10"));

        // Chain Geometry connecting pins
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

        // Draw Shield
        if (shieldActive) {
            canvas.drawCircle(centerX, centerY, coreRadius + 40f, paintShield);
        }
        
        // Draw Core (Changes color if risk mode is active)
        if (riskMode) {
            paintCore.setColor(Color.parseColor("#FF4757"));
            paintCore.setShadowLayer(40f, 0, 0, Color.parseColor("#FF4757"));
        } else {
            paintCore.setColor(Color.parseColor("#00D2D3"));
            paintCore.setShadowLayer(40f, 0, 0, Color.parseColor("#00D2D3"));
        }
        canvas.drawCircle(centerX, centerY, coreRadius, paintCore);
        
        // Inner Details of Core
        Paint pDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        pDark.setColor(Color.parseColor("#1a1a2e"));
        canvas.drawCircle(centerX, centerY, coreRadius * 0.7f, pDark);
        canvas.drawCircle(centerX, centerY, coreRadius * 0.3f, paintCore);

        // Draw Pulse Window Target
        if (currentLevelData != null && currentLevelData.pulseSpeed > 0) {
            float pr = (float) Math.toRadians(pulseRotation);
            float px = centerX + (float) Math.cos(pr) * coreRadius;
            float py = centerY + (float) Math.sin(pr) * coreRadius;
            canvas.drawCircle(px, py, 20f, paintPulse);
            
            // Outer Ring pulsing
            Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);
            pRing.setStyle(Paint.Style.STROKE);
            pRing.setColor(Color.parseColor("#FF9F43"));
            pRing.setStrokeWidth(4f);
            canvas.drawCircle(px, py, 30f + (float)(Math.sin(frameCount * 0.15) * 10), pRing);
        }

        // Draw Attached Pins
        for (Float a : pinsOnCore) {
            float angle = (float) Math.toRadians(a + coreRotation);
            float startX = centerX + (float) Math.cos(angle) * coreRadius;
            float startY = centerY + (float) Math.sin(angle) * coreRadius;
            float endX = centerX + (float) Math.cos(angle) * (coreRadius + pinLength);
            float endY = centerY + (float) Math.sin(angle) * (coreRadius + pinLength);
            
            canvas.drawLine(startX, startY, endX, endY, paintPinLine);
            paintPinHead.setColor(Color.WHITE);
            canvas.drawCircle(endX, endY, 15f, paintPinHead);
        }

        // Draw Flying Pin
        if (flyingPinY != null) {
            paintPinLine.setColor(isRiskPin ? Color.parseColor("#FF4757") : Color.WHITE);
            canvas.drawLine(centerX, flyingPinY, centerX, flyingPinY + pinLength, paintPinLine);
            
            paintPinHead.setColor(isRiskPin ? Color.parseColor("#FF4757") : Color.WHITE);
            canvas.drawCircle(centerX, flyingPinY + pinLength, 15f, paintPinHead);
            paintPinLine.setColor(Color.WHITE); // Reset color
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
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }
    public void setRunning(boolean isRunning) { running = isRunning; }

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
            } catch (Exception e) {} 
            finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas); } catch (Exception e) {}
                }
            }
            try { sleep(16); } catch (Exception e) {} // Approx 60 FPS
        }
    }
}
