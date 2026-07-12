package com.orbita.flowarena;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private GameView gameView;
    
    private LinearLayout menuUI, resultUI;
    private RelativeLayout hudUI;
    private TextView lblLevel, lblPins, lblResultTitle;
    private Button btnPlay, btnNext, btnShield, btnRisk;

    private int currentLevelId = 1;
    private int shields = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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

        // Force game view to be visible behind the menu initially
        gameView.setVisibility(View.VISIBLE);

        btnPlay.setOnClickListener(v -> startGame());
        btnNext.setOnClickListener(v -> {
            if (lblResultTitle.getText().toString().contains("GEÇİLDİ")) {
                currentLevelId++;
                startGame();
            } else {
                resultUI.setVisibility(View.GONE);
                menuUI.setVisibility(View.VISIBLE);
            }
        });

        btnRisk.setOnClickListener(v -> {
            gameView.riskMode = !gameView.riskMode;
            btnRisk.setBackgroundColor(gameView.riskMode ? Color.parseColor("#ff4757") : Color.DKGRAY);
            btnRisk.setTextColor(gameView.riskMode ? Color.WHITE : Color.parseColor("#ff4757"));
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
        gameView.setVisibility(View.VISIBLE);
        gameView.bringToFront();
        hudUI.bringToFront();
        
        LevelData data = LevelData.getLevel(currentLevelId);
        lblLevel.setText("LEVEL " + currentLevelId + ": " + data.name);
        
        updatePinsUI(data.targetPins);
        gameView.startLevel(currentLevelId);
    }

    public void updatePinsUI(int count) {
        runOnUiThread(() -> lblPins.setText("Kalan: " + count));
    }
    
    public void showToast(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    public void showResult(boolean isWin) {
        hudUI.setVisibility(View.GONE);
        resultUI.setVisibility(View.VISIBLE);
        resultUI.bringToFront();
        if (isWin) {
            lblResultTitle.setText("LEVEL GEÇİLDİ!");
            lblResultTitle.setTextColor(Color.parseColor("#1dd1a1"));
            btnNext.setText("SONRAKİ LEVEL");
        } else {
            lblResultTitle.setText("ÇARPIŞMA!");
            lblResultTitle.setTextColor(Color.parseColor("#ff4757"));
            btnNext.setText("MENÜYE DÖN");
        }
    }
}
