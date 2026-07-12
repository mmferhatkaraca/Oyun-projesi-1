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
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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

        // Ensure GameView is rendering in background
        gameView.setVisibility(View.VISIBLE);

        btnPlay.setOnClickListener(v -> startGame());
        btnNext.setOnClickListener(v -> {
            if (lblResultTitle.getText().toString().contains("TAMAMLANDI")) {
                currentLevelId++;
                startGame();
            } else {
                resultUI.setVisibility(View.GONE);
                menuUI.setVisibility(View.VISIBLE);
            }
        });

        btnRisk.setOnClickListener(v -> {
            gameView.toggleRiskMode();
            boolean isRisk = gameView.isRiskMode();
            btnRisk.setBackgroundColor(isRisk ? Color.parseColor("#C0392B") : Color.parseColor("#FF4757"));
            btnRisk.setText(isRisk ? "RİSK: AKTİF" : "RİSK MODU");
        });

        btnShield.setOnClickListener(v -> {
            if (shields > 0 && !gameView.isShieldActive()) {
                shields--;
                gameView.activateShield();
                btnShield.setText("KALKAN (" + shields + ")");
            } else if (shields <= 0) {
                showToast("Kalkan kalmadı!");
            }
        });
    }

    private void startGame() {
        menuUI.setVisibility(View.GONE);
        resultUI.setVisibility(View.GONE);
        hudUI.setVisibility(View.VISIBLE);
        
        LevelData data = LevelData.getLevel(currentLevelId);
        lblLevel.setText("LEVEL " + currentLevelId);
        
        updatePinsUI(data.targetPins);
        
        // Reset Risk UI
        btnRisk.setBackgroundColor(Color.parseColor("#FF4757"));
        btnRisk.setText("RİSK MODU");

        gameView.startLevel(data);
    }

    public void updatePinsUI(int count) {
        runOnUiThread(() -> lblPins.setText(String.valueOf(count)));
    }
    
    public void showToast(String msg) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    public void onLevelEnded(boolean isWin) {
        runOnUiThread(() -> {
            hudUI.setVisibility(View.GONE);
            resultUI.setVisibility(View.VISIBLE);
            if (isWin) {
                lblResultTitle.setText("LEVEL TAMAMLANDI!");
                lblResultTitle.setTextColor(Color.parseColor("#1DD1A1"));
                lblResultTitle.setShadowLayer(20, 0, 0, Color.parseColor("#1DD1A1"));
                btnNext.setText("SONRAKİ LEVEL");
            } else {
                lblResultTitle.setText("ÇARPIŞMA!");
                lblResultTitle.setTextColor(Color.parseColor("#FF4757"));
                lblResultTitle.setShadowLayer(20, 0, 0, Color.parseColor("#FF4757"));
                btnNext.setText("MENÜYE DÖN");
            }
        });
    }
}
