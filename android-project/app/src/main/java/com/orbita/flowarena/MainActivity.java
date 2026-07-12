package com.orbita.flowarena;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private GameView gameView;
    
    private LinearLayout menuUI, resultUI;
    private RelativeLayout hudUI;
    private TextView lblLevel, lblPins, lblResultTitle;
    private Button btnPlay, btnNext, btnShield, btnRisk;

    private int currentLevel = 1;
    private int shields = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fullscreen Immersive
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
        
        setContentView(R.layout.activity_main);

        gameView = findViewById(R.id.gameView);
        gameView.setActivity(this);

        menuUI = findViewById(R.id.menuUI);
        hudUI = findViewById(R.id.hudUI);
        resultUI = findViewById(R.id.resultUI);
        
        lblLevel = findViewById(R.id.lblLevel);
        lblPins = findViewById(R.id.lblPins);
        lblResultTitle = findViewById(R.id.lblResultTitle);
        
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);
        btnShield = findViewById(R.id.btnShield);
        btnRisk = findViewById(R.id.btnRisk);

        btnPlay.setOnClickListener(v -> startGame());
        btnNext.setOnClickListener(v -> {
            if (lblResultTitle.getText().toString().contains("GEÇİLDİ")) {
                currentLevel++;
                startGame();
            } else {
                resultUI.setVisibility(View.GONE);
                menuUI.setVisibility(View.VISIBLE);
            }
        });

        btnRisk.setOnClickListener(v -> {
            gameView.riskMode = !gameView.riskMode;
            btnRisk.setBackgroundColor(gameView.riskMode ? Color.parseColor("#ff4757") : Color.DKGRAY);
        });

        btnShield.setOnClickListener(v -> {
            if (shields > 0 && !gameView.shieldActive) {
                shields--;
                gameView.shieldActive = true;
                btnShield.setText("KALKAN (" + shields + ")");
            }
        });
    }

    private void startGame() {
        menuUI.setVisibility(View.GONE);
        resultUI.setVisibility(View.GONE);
        hudUI.setVisibility(View.VISIBLE);
        
        lblLevel.setText("LEVEL " + currentLevel);
        int targetPins = 5 + (currentLevel * 2);
        float speed = 1.0f + (currentLevel * 0.2f);
        
        updatePinsUI(targetPins);
        gameView.startLevel(targetPins, speed);
    }

    public void updatePinsUI(int count) {
        runOnUiThread(() -> lblPins.setText("Kalan: " + count));
    }

    public void showResult(boolean isWin) {
        hudUI.setVisibility(View.GONE);
        resultUI.setVisibility(View.VISIBLE);
        if (isWin) {
            lblResultTitle.setText("LEVEL GEÇİLDİ!");
            lblResultTitle.setTextColor(Color.parseColor("#1dd1a1"));
        } else {
            lblResultTitle.setText("ÇARPIŞMA!");
            lblResultTitle.setTextColor(Color.parseColor("#ff4757"));
        }
    }
}
