package com.orbita.flowarena;

public class LevelData {
    public int id;
    public String name;
    public float speed;
    public int targetPins;
    public float pulseSpeed;
    public int reverseTime;

    public LevelData(int id, String name, float speed, int targetPins, float pulseSpeed, int reverseTime) {
        this.id = id;
        this.name = name;
        this.speed = speed;
        this.targetPins = targetPins;
        this.pulseSpeed = pulseSpeed;
        this.reverseTime = reverseTime;
    }

    public static LevelData getLevel(int levelId) {
        switch (levelId) {
            case 1: return new LevelData(1, "İlk Işık", 1.0f, 6, 0f, 0);
            case 2: return new LevelData(2, "Saat Yönü", -1.2f, 8, 0f, 0);
            case 3: return new LevelData(3, "Ters Akış", 1.5f, 10, 0f, 180);
            case 4: return new LevelData(4, "Pulse Penceresi", 1.5f, 12, 1.8f, 0);
            case 5: return new LevelData(5, "Risk Hattı", 1.8f, 14, 2.0f, 0);
            case 6: return new LevelData(6, "Hız Dalgası", 2.0f, 15, 2.2f, 0);
            case 7: return new LevelData(7, "Üçlü Zincir", 1.8f, 16, 2.5f, 200);
            case 8: return new LevelData(8, "Kalkan Eşiği", -2.0f, 18, 2.8f, 150);
            case 9: return new LevelData(9, "İki Faz", 2.2f, 20, -1.5f, 120);
            case 10: return new LevelData(10, "Atlas Boss", 2.5f, 25, 3.0f, 100);
            default: return new LevelData(levelId, "Sonsuz Uzay", 2.5f + (levelId * 0.1f), 15 + levelId, 3.0f, 100);
        }
    }
}
