package com.orbita.rokclone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread thread;
    private MainActivity activity;

    private float camX = 500f;
    private float camY = 200f;
    private float lastTouchX, lastTouchY;

    private final int MAP_SIZE = 15;
    private final int TILE_W = 160;
    private final int TILE_H = 80;

    private static class Building {
        String type;
        int gridX, gridY;
        Building(String t, int x, int y) { type = t; gridX = x; gridY = y; }
    }
    private List<Building> buildings = new ArrayList<>();
    private String buildMode = null;

    private Paint paintGrassDark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintGrassLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTownhall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintFarm = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);

        paintGrassDark.setColor(Color.parseColor("#2d5a27"));
        paintGrassLight.setColor(Color.parseColor("#3a7332"));
        
        paintGrid.setColor(Color.parseColor("#1A000000"));
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setStrokeWidth(2f);

        paintTownhall.setColor(Color.parseColor("#4a69bd"));
        paintFarm.setColor(Color.parseColor("#f6b93b"));

        buildings.add(new Building("townhall", 7, 7));
    }

    public void setActivity(MainActivity act) { this.activity = act; }
    public void enterBuildMode(String type) { this.buildMode = type; }
    
    public int getBuildingCount(String type) {
        int c = 0;
        for(Building b : buildings) { if(b.type.equals(type)) c++; }
        return c;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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
            try { thread.join(); retry = false; } catch (InterruptedException e) {}
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                break;
                
            case MotionEvent.ACTION_MOVE:
                camX += (x - lastTouchX);
                camY += (y - lastTouchY);
                lastTouchX = x;
                lastTouchY = y;
                break;
                
            case MotionEvent.ACTION_UP:
                if (buildMode != null) {
                    float adjX = x - camX;
                    float adjY = y - camY;
                    
                    int gX = (int) Math.floor((adjX / (TILE_W / 2.0f) + adjY / (TILE_H / 2.0f)) / 2.0f);
                    int gY = (int) Math.floor((adjY / (TILE_H / 2.0f) - adjX / (TILE_W / 2.0f)) / 2.0f);

                    if (gX >= 0 && gX < MAP_SIZE && gY >= 0 && gY < MAP_SIZE) {
                        boolean occupied = false;
                        for(Building b : buildings) {
                            if(b.gridX == gX && b.gridY == gY) occupied = true;
                        }
                        
                        if (!occupied) {
                            if (activity.spendWood(100)) {
                                buildings.add(new Building(buildMode, gX, gY));
                                buildMode = null;
                            }
                        }
                    }
                }
                break;
        }
        return true;
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        canvas.drawColor(Color.parseColor("#1e272e")); // Arkaplan boşluk

        // Zemin Çizimi (Native Vector Isometric)
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                float px = camX + (x - y) * (TILE_W / 2f);
                float py = camY + (x + y) * (TILE_H / 2f);

                if (px < -200 || px > getWidth() + 200 || py < -200 || py > getHeight() + 200) continue;

                Path path = new Path();
                path.moveTo(px, py - TILE_H/2f);
                path.lineTo(px + TILE_W/2f, py);
                path.lineTo(px, py + TILE_H/2f);
                path.lineTo(px - TILE_W/2f, py);
                path.close();

                Paint p = ((x + y) % 2 == 0) ? paintGrassDark : paintGrassLight;
                canvas.drawPath(path, p);
                canvas.drawPath(path, paintGrid);
            }
        }

        // Binaları Sırala ve Çiz
        List<Building> sorted = new ArrayList<>(buildings);
        sorted.sort((b1, b2) -> Integer.compare(b1.gridX + b1.gridY, b2.gridX + b2.gridY));

        for (Building b : sorted) {
            float px = camX + (b.gridX - b.gridY) * (TILE_W / 2f);
            float py = camY + (b.gridX + b.gridY) * (TILE_H / 2f);

            if (b.type.equals("townhall")) {
                // Belediye Binası Vektörü
                Path th = new Path();
                th.moveTo(px, py - 40);
                th.lineTo(px + 60, py);
                th.lineTo(px, py + 40);
                th.lineTo(px - 60, py);
                th.close();
                canvas.drawPath(th, paintTownhall);
                
                // Çatı
                Path roof = new Path();
                roof.moveTo(px - 60, py);
                roof.lineTo(px, py - 120);
                roof.lineTo(px + 60, py);
                roof.close();
                Paint rp = new Paint(); rp.setColor(Color.parseColor("#1e3799"));
                canvas.drawPath(roof, rp);
                
            } else if (b.type.equals("farm")) {
                // Çiftlik Vektörü
                Path fm = new Path();
                fm.moveTo(px, py - 20);
                fm.lineTo(px + 40, py);
                fm.lineTo(px, py + 20);
                fm.lineTo(px - 40, py);
                fm.close();
                canvas.drawPath(fm, paintFarm);
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
                    this.gameView.draw(canvas);
                }
            } catch (Exception e) {} 
            finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas); } catch (Exception e) {}
                }
            }
        }
    }
}
