package com.orbita.rokclone;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private GameView gameView;
    private TextView txtFood, txtWood;
    
    public int food = 500;
    public int wood = 1000;

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

        txtFood = findViewById(R.id.txtFood);
        txtWood = findViewById(R.id.txtWood);
        Button btnBuildFarm = findViewById(R.id.btnBuildFarm);

        btnBuildFarm.setOnClickListener(v -> {
            if (wood >= 100) {
                gameView.enterBuildMode("farm");
                Toast.makeText(this, "Haritaya dokunarak Çiftliği yerleştirin.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Yetersiz Odun!", Toast.LENGTH_SHORT).show();
            }
        });

        new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(1000);
                    int farmCount = gameView.getBuildingCount("farm");
                    if (farmCount > 0) {
                        food += farmCount * 10;
                        updateUI();
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    public void updateUI() {
        runOnUiThread(() -> {
            txtFood.setText("YİYECEK: " + food);
            txtWood.setText("ODUN: " + wood);
        });
    }

    public boolean spendWood(int amount) {
        if (wood >= amount) {
            wood -= amount;
            updateUI();
            return true;
        }
        return false;
    }
}
