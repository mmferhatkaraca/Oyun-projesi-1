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
    private float coreRadius = 150f;
    private float pinLength = 120f;
    
    public int pinsLeft = 0;
    private float coreRotation = 0f;
    private float pulseRotation = 0f;
    private LevelData currentLevelData;
    
    private List<Float> pinsOnCore = new ArrayList<>();
    private Float flyingPinY = null;
    private boolean isRiskPin = false;

    public boolean isPlaying = false;
    public boolean riskMode = false;
    public boolean shieldActive = false;
    
    private int frameCount = 0;
    private int combo = 0;
    
    private Paint paintCore = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintCoreGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPinLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPinHead = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintPulse = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintChain = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintShield = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        
        paintCore.setStyle(Paint.Style.FILL);
        paintCoreGlow.setStyle(Paint.Style.FILL);
        
        paintPinLine.setColor(Color.WHITE);
        paintPinLine.setStrokeWidth(6f);
        paintPinLine.setStrokeCap(Paint.Cap.ROUND);
        
        paintPinHead.setStyle(Paint.Style.FILL);
        
        paintPulse.setColor(Color.parseColor("#ff9f43")); // Amber
        paintPulse.setStyle(Paint.Style.FILL);
        paintPulse.setShadowLayer(25f, 0, 0, Color.parseColor("#ff9f43"));
        setLayerType(LAYER_TYPE_SOFTWARE, paintPulse); // Required for hardware shadow
        
        paintChain.setColor(Color.parseColor("#8800d2d3")); // Semi-transparent Turquoise
        paintChain.setStrokeWidth(3f);
        
        paintShield.setColor(Color.parseColor("#331dd1a1")); // Transparent Green
        paintShield.setStyle(Paint.Style.FILL);
    }

    public void setActivity(MainActivity act) {
        this.activity = act;
    }

    public void startLevel(int levelId) {
        this.currentLevelData = LevelData.getLevel(levelId);
        this.pinsLeft = currentLevelData.targetPins;
        this.pinsOnCore.clear();
        this.coreRotation = 0f;
        this.pulseRotation = 0f;
        this.flyingPinY = null;
        this.frameCount = 0;
        this.combo = 0;
        this.isPlaying = true;
        this.shieldActive = false;
        this.riskMode = false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        width = getWidth();
        height = getHeight();
        centerX = width / 2;
        centerY = (int)(height * 0.35); // Core slightly above center
        
        updateGlow();
        
        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }
    
    private void updateGlow() {
        if (width > 0 && height > 0) {
            int glowColor = riskMode ? Color.parseColor("#66ff4757") : Color.parseColor("#6600d2d3");
            RadialGradient gradient = new RadialGradient(centerX, centerY, coreRadius * 2, 
                glowColor, Color.TRANSPARENT, Shader.TileMode.CLAMP);
            paintCoreGlow.setShader(gradient);
        }
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
                flyingPinY = (float) height - 300f; // Start position
                isRiskPin = riskMode;
                vibrate(15);
            }
        }
        return true;
    }

    public void update() {
        if (!isPlaying || currentLevelData == null) return;
        frameCount++;

        // Reverse Logic
        if (currentLevelData.reverseTime > 0 && frameCount % currentLevelData.reverseTime == 0) {
            currentLevelData.speed *= -1;
            activity.showToast("YÖN DEĞİŞTİ!");
        }

        coreRotation = (coreRotation + currentLevelData.speed) % 360f;
        pulseRotation = (pulseRotation + currentLevelData.pulseSpeed) % 360f;
        
        if (frameCount % 60 == 0) {
           updateGlow(); // Refresh colors if needed
        }

        if (flyingPinY != null) {
            flyingPinY -= 70f; // Pin speed
            float targetY = centerY + coreRadius;
            
            if (flyingPinY <= targetY) {
                // Collision Logic
                float attachAngle = (360f - coreRotation) % 360f;
                float safeDist = isRiskPin ? 18f : 12f; // degrees (harder in risk mode)
                
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
                        activity.runOnUiThread(() -> activity.showResult(false));
                    }
                } else {
                    // Check Pulse Perfect
                    float pulseAngleReal = (360f - pulseRotation) % 360f;
                    float pulseDiff = Math.abs(attachAngle - pulseAngleReal);
                    if (pulseDiff > 180f) pulseDiff = 360f - pulseDiff;
                    
                    if (currentLevelData.pulseSpeed > 0 && pulseDiff < 15f) { // Perfect Window
                        combo++;
                        activity.showToast("PERFECT x" + combo);
                        vibrate(50);
                    } else {
                        combo = 0;
                        vibrate(10);
                    }
                    
                    pinsOnCore.add(attachAngle);
                    if (pinsLeft <= 0) {
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
        
        // Deep Space Background
        canvas.drawColor(Color.parseColor("#0b0c10"));
        
        // Draw Ambient Glow
        canvas.drawCircle(centerX, centerY, coreRadius * 2, paintCoreGlow);

        // Draw Chain Geometry (Anatolian sci-fi connecting lines)
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
            canvas.drawCircle(centerX, centerY, coreRadius + 25f, paintShield);
        }
        
        // Draw Core
        paintCore.setColor(riskMode ? Color.parseColor("#ff4757") : Color.parseColor("#00d2d3"));
        canvas.drawCircle(centerX, centerY, coreRadius, paintCore);
        // Core Inner Detail
        paintCore.setColor(Color.parseColor("#1a1a2e"));
        canvas.drawCircle(centerX, centerY, coreRadius * 0.7f, paintCore);
        paintCore.setColor(riskMode ? Color.parseColor("#ff4757") : Color.parseColor("#00d2d3"));
        canvas.drawCircle(centerX, centerY, coreRadius * 0.3f, paintCore);

        // Draw Pulse Target (Amber)
        if (currentLevelData != null && currentLevelData.pulseSpeed > 0) {
            float pr = (float) Math.toRadians(pulseRotation);
            float px = centerX + (float) Math.cos(pr) * coreRadius;
            float py = centerY + (float) Math.sin(pr) * coreRadius;
            canvas.drawCircle(px, py, 18f, paintPulse);
            
            // Pulse outer ring
            Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);
            pRing.setStyle(Paint.Style.STROKE);
            pRing.setColor(Color.parseColor("#ff9f43"));
            pRing.setStrokeWidth(3f);
            canvas.drawCircle(px, py, 26f + (float)(Math.sin(frameCount * 0.2) * 5), pRing);
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
            canvas.drawCircle(endX, endY, 12f, paintPinHead);
        }

        // Draw Flying Pin
        if (flyingPinY != null) {
            paintPinLine.setColor(isRiskPin ? Color.parseColor("#ff4757") : Color.WHITE);
            canvas.drawLine(centerX, flyingPinY, centerX, flyingPinY + pinLength, paintPinLine);
            
            paintPinHead.setColor(isRiskPin ? Color.parseColor("#ff4757") : Color.WHITE);
            canvas.drawCircle(centerX, flyingPinY + pinLength, 12f, paintPinHead);
            paintPinLine.setColor(Color.WHITE); // Reset
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
