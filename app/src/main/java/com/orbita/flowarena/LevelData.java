package com.orbita.flowarena;

public class LevelData {
    public int id;
    public String name;
    public float baseSpeed;
    public int targetPins;
    public float pulseSpeed;
    public int reverseFrameInterval;

    public LevelData(int id, String name, float baseSpeed, int targetPins, float pulseSpeed, int reverseFrameInterval) {
        this.id = id;
        this.name = name;
        this.baseSpeed = baseSpeed;
        this.targetPins = targetPins;
        this.pulseSpeed = pulseSpeed;
        this.reverseFrameInterval = reverseFrameInterval;
    }

    public static LevelData getLevel(int levelId) {
        switch (levelId) {
            case 1: return new LevelData(1, "Uyanış", 1.2f, 6, 0f, 0);
            case 2: return new LevelData(2, "İvme", 1.8f, 10, 0f, 0);
            case 3: return new LevelData(3, "Ters Akım", 1.5f, 12, 0f, 180); // Reverses every 3 seconds
            case 4: return new LevelData(4, "Pulse Penceresi", 1.5f, 12, 2.0f, 0);
            case 5: return new LevelData(5, "Risk Eşiği", 2.2f, 15, 2.5f, 0);
            case 6: return new LevelData(6, "Kaos", 2.5f, 18, -1.5f, 150);
            case 7: return new LevelData(7, "Zincirleme", 2.0f, 20, 3.0f, 120);
            case 8: return new LevelData(8, "Kalkan Testi", -2.8f, 22, 3.5f, 0);
            case 9: return new LevelData(9, "İlizyon", 3.2f, 25, -2.5f, 90);
            case 10: return new LevelData(10, "Atlas Çekirdeği", 3.5f, 30, 4.0f, 120);
            default: return new LevelData(levelId, "Sonsuz Boşluk", 3.5f + (levelId * 0.1f), 20 + levelId, 4.0f, 100);
        }
    }
}
