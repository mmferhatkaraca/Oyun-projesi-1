const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');

let W, H, CENTER_X, CENTER_Y;
function resize() {
    W = window.innerWidth; H = window.innerHeight;
    canvas.width = W; canvas.height = H;
    CENTER_X = W / 2; CENTER_Y = H * 0.35; // Core is slightly above middle
}
window.addEventListener('resize', resize);
resize();

// UI Elements
const menuUI = document.getElementById('menuUI');
const resultUI = document.getElementById('resultUI');
const hud = document.getElementById('hud');
const jokers = document.getElementById('jokers');
const lblLevel = document.getElementById('lblLevel');
const lblPins = document.getElementById('lblPins');
const messageBox = document.getElementById('messageBox');
const resTitle = document.getElementById('resTitle');
const lblShieldCount = document.getElementById('lblShieldCount');
const lblSlowCount = document.getElementById('lblSlowCount');

// Game Data
const LEVELS = [
    { id: 1, name: "İlk Işık", speed: 0.02, target_pins: 6, pulseSpeed: 0, reverseTime: 0 },
    { id: 2, name: "Saat Yönü", speed: -0.03, target_pins: 8, pulseSpeed: 0, reverseTime: 0 },
    { id: 3, name: "Ters Akış", speed: 0.035, target_pins: 10, pulseSpeed: 0, reverseTime: 180 },
    { id: 4, name: "Pulse Penceresi", speed: 0.03, target_pins: 12, pulseSpeed: 0.05, reverseTime: 0 },
    { id: 5, name: "Hız Dalgası", speed: 0.04, target_pins: 14, pulseSpeed: 0.06, reverseTime: 0 },
    { id: 6, name: "Atlas Boss", speed: 0.045, target_pins: 20, pulseSpeed: 0.07, reverseTime: 150 }
];

let gameState = 'MENU'; // MENU, PLAYING, RESULT
let currentLevelIdx = 0;
let save_data = { level: 1, shields: 3, slows: 3 };

// Mechanics state
let coreRotation = 0;
let pulseRotation = 0;
let pinsOnCore = []; // angles
let flyingPin = null; // {y, isRisk}
let pinsLeft = 0;
let currentLevel = null;
let frameCount = 0;

// Jokers & Risk
let riskMode = false;
let shieldActive = false;
let slowTimer = 0;
let combo = 0;

// Audio Synthesizer (Sci-Fi Sounds)
const AudioContext = window.AudioContext || window.webkitAudioContext;
let audioCtx;
function playSound(type) {
    if(!audioCtx) audioCtx = new AudioContext();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain); gain.connect(audioCtx.destination);
    
    if (type === 'shoot') {
        osc.type = 'sine'; osc.frequency.setValueAtTime(800, audioCtx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(300, audioCtx.currentTime + 0.1);
        gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.1);
        osc.start(); osc.stop(audioCtx.currentTime + 0.1);
    } else if (type === 'hit') {
        osc.type = 'triangle'; osc.frequency.setValueAtTime(200, audioCtx.currentTime);
        gain.gain.setValueAtTime(0.5, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.1);
        osc.start(); osc.stop(audioCtx.currentTime + 0.1);
    } else if (type === 'crash') {
        osc.type = 'sawtooth'; osc.frequency.setValueAtTime(100, audioCtx.currentTime);
        osc.frequency.linearRampToValueAtTime(50, audioCtx.currentTime + 0.3);
        gain.gain.setValueAtTime(0.8, audioCtx.currentTime);
        gain.gain.linearRampToValueAtTime(0.01, audioCtx.currentTime + 0.3);
        osc.start(); osc.stop(audioCtx.currentTime + 0.3);
    } else if (type === 'perfect') {
        osc.type = 'sine'; osc.frequency.setValueAtTime(1000, audioCtx.currentTime);
        osc.frequency.linearRampToValueAtTime(1500, audioCtx.currentTime + 0.1);
        gain.gain.setValueAtTime(0.5, audioCtx.currentTime);
        gain.gain.linearRampToValueAtTime(0.01, audioCtx.currentTime + 0.2);
        osc.start(); osc.stop(audioCtx.currentTime + 0.2);
    }
}

function vibrate(ms) {
    if (navigator.vibrate) navigator.vibrate(ms);
}

function showMsg(msg, color) {
    messageBox.innerText = msg;
    messageBox.style.color = color;
    messageBox.style.textShadow = `0 0 10px ${color}`;
    messageBox.style.opacity = 1;
    setTimeout(() => { messageBox.style.opacity = 0; }, 1000);
}

function startLevel(idx, isTutorial = false) {
    if(idx >= LEVELS.length) idx = 0;
    currentLevelIdx = idx;
    currentLevel = isTutorial ? { id: 0, name: "Eğitim", speed: 0.01, target_pins: 3, pulseSpeed: 0, reverseTime: 0 } : LEVELS[idx];
    
    pinsLeft = currentLevel.target_pins;
    pinsOnCore = [];
    coreRotation = 0; pulseRotation = 0;
    riskMode = false; shieldActive = false; slowTimer = 0; combo = 0;
    
    lblLevel.innerText = currentLevel.id;
    lblPins.innerText = pinsLeft;
    updateJokerUI();
    
    menuUI.classList.add('hidden');
    resultUI.classList.add('hidden');
    hud.classList.remove('hidden');
    jokers.classList.remove('hidden');
    gameState = 'PLAYING';
}

function updateJokerUI() {
    lblShieldCount.innerText = save_data.shields;
    lblSlowCount.innerText = save_data.slows;
    document.getElementById('btnRisk').style.background = riskMode ? '#ff4757' : 'transparent';
    document.getElementById('btnRisk').style.color = riskMode ? '#0b0c10' : '#ff4757';
}

// Button Events
document.getElementById('btnPlay').onclick = () => startLevel(save_data.level - 1);
document.getElementById('btnTutorial').onclick = () => startLevel(0, true);
document.getElementById('btnNext').onclick = () => {
    if (gameState === 'RESULT_WIN') {
        save_data.level++;
        startLevel(currentLevelIdx + 1);
    } else {
        menuUI.classList.remove('hidden');
        resultUI.classList.add('hidden');
    }
};

document.getElementById('btnRisk').onclick = (e) => { e.stopPropagation(); riskMode = !riskMode; updateJokerUI(); playSound('perfect'); showMsg(riskMode ? "RİSK AÇIK!" : "RİSK KAPALI", "#ff4757"); };
document.getElementById('btnShield').onclick = (e) => {
    e.stopPropagation();
    if(save_data.shields > 0 && !shieldActive) {
        save_data.shields--; shieldActive = true; updateJokerUI();
        playSound('perfect'); showMsg("KALKAN AKTİF!", "#1dd1a1");
    }
};
document.getElementById('btnSlow').onclick = (e) => {
    e.stopPropagation();
    if(save_data.slows > 0) {
        save_data.slows--; slowTimer = 180; updateJokerUI();
        playSound('perfect'); showMsg("ZAMAN YAVAŞLADI", "#00d2d3");
    }
};

// Gameplay Input
canvas.addEventListener('touchstart', handleTap);
canvas.addEventListener('mousedown', handleTap);

function handleTap(e) {
    if (e.target.tagName === 'BUTTON') return;
    if (gameState === 'PLAYING' && !flyingPin && pinsLeft > 0) {
        pinsLeft--;
        lblPins.innerText = pinsLeft;
        flyingPin = { y: H - 100, isRisk: riskMode };
        playSound('shoot');
    }
}

function gameOver(win) {
    gameState = win ? 'RESULT_WIN' : 'RESULT_LOSS';
    hud.classList.add('hidden');
    jokers.classList.add('hidden');
    resultUI.classList.remove('hidden');
    if (win) {
        resTitle.innerText = "LEVEL GEÇİLDİ!";
        resTitle.style.color = "#1dd1a1";
        resTitle.style.textShadow = "0 0 20px #1dd1a1";
        playSound('perfect'); vibrate(100);
    } else {
        resTitle.innerText = "ÇARPIŞMA!";
        resTitle.style.color = "#ff4757";
        resTitle.style.textShadow = "0 0 20px #ff4757";
        playSound('crash'); vibrate([200, 100, 200]);
    }
}

// Main Loop
function loop() {
    requestAnimationFrame(loop);
    
    // Logic
    if (gameState === 'PLAYING') {
        frameCount++;
        let currentSpeed = currentLevel.speed;
        
        // Reverse Logic
        if (currentLevel.reverseTime > 0 && frameCount % currentLevel.reverseTime === 0) {
            currentLevel.speed *= -1;
            showMsg("YÖN DEĞİŞTİ!", "#00d2d3");
        }
        
        // Slow Joker Logic
        if (slowTimer > 0) {
            slowTimer--;
            currentSpeed *= 0.3;
        }
        
        coreRotation += currentSpeed;
        pulseRotation += currentLevel.pulseSpeed;
        
        if (flyingPin) {
            flyingPin.y -= 25; // Speed
            let coreRadius = 50;
            let pinTargetY = CENTER_Y + coreRadius;
            
            if (flyingPin.y <= pinTargetY) {
                // Pin hit the core
                let attachAngle = (-coreRotation + Math.PI * 2) % (Math.PI * 2);
                let safeDist = flyingPin.isRisk ? 0.35 : 0.22; // Radians
                
                let collided = false;
                for (let a of pinsOnCore) {
                    let diff = Math.abs(a - attachAngle);
                    if (diff > Math.PI) diff = Math.PI * 2 - diff;
                    if (diff < safeDist) { collided = true; break; }
                }
                
                if (collided) {
                    if (shieldActive) {
                        shieldActive = false;
                        showMsg("KALKAN KIRILDI!", "#ff4757");
                        vibrate(100); playSound('hit');
                    } else {
                        gameOver(false);
                    }
                } else {
                    // Check Pulse
                    let pulseAngleReal = (-pulseRotation + Math.PI * 2) % (Math.PI * 2);
                    let pulseDiff = Math.abs(attachAngle - pulseAngleReal);
                    if (pulseDiff > Math.PI) pulseDiff = Math.PI * 2 - pulseDiff;
                    
                    if (currentLevel.pulseSpeed > 0 && pulseDiff < 0.3) {
                        combo++;
                        showMsg("PERFECT x" + combo, "#ff9f43");
                        playSound('perfect'); vibrate(50);
                    } else {
                        combo = 0;
                        playSound('hit'); vibrate(20);
                    }
                    
                    pinsOnCore.push(attachAngle);
                    if (pinsLeft === 0) {
                        setTimeout(() => gameOver(true), 500);
                    }
                }
                flyingPin = null;
            }
        }
    }
    
    // Draw
    ctx.clearRect(0, 0, W, H);
    
    // Draw Connections (Anatolian Geometry / Chain)
    if (gameState === 'PLAYING' || gameState.startsWith('RESULT')) {
        let coreRadius = 50;
        let pinLen = 40;
        
        if (pinsOnCore.length >= 3) {
            ctx.beginPath();
            for (let i = 0; i < pinsOnCore.length; i++) {
                let a = pinsOnCore[i] + coreRotation;
                let px = CENTER_X + Math.cos(a) * (coreRadius + pinLen);
                let py = CENTER_Y + Math.sin(a) * (coreRadius + pinLen);
                if (i===0) ctx.moveTo(px, py); else ctx.lineTo(px, py);
            }
            ctx.closePath();
            ctx.strokeStyle = "rgba(0, 210, 211, 0.3)";
            ctx.lineWidth = 2;
            ctx.stroke();
        }
        
        // Draw Shield
        if (shieldActive) {
            ctx.beginPath();
            ctx.arc(CENTER_X, CENTER_Y, coreRadius + 10, 0, Math.PI * 2);
            ctx.fillStyle = "rgba(29, 209, 161, 0.2)";
            ctx.fill();
        }
        
        // Draw Core
        ctx.beginPath();
        ctx.arc(CENTER_X, CENTER_Y, coreRadius, 0, Math.PI * 2);
        ctx.fillStyle = riskMode ? "#ff4757" : "#00d2d3";
        ctx.shadowColor = ctx.fillStyle;
        ctx.shadowBlur = 20;
        ctx.fill();
        ctx.shadowBlur = 0;
        
        // Draw Pulse Dot
        if (currentLevel && currentLevel.pulseSpeed > 0) {
            let px = CENTER_X + Math.cos(pulseRotation) * coreRadius;
            let py = CENTER_Y + Math.sin(pulseRotation) * coreRadius;
            ctx.beginPath();
            ctx.arc(px, py, 8, 0, Math.PI * 2);
            ctx.fillStyle = "#ff9f43";
            ctx.shadowColor = "#ff9f43";
            ctx.shadowBlur = 15;
            ctx.fill();
            ctx.shadowBlur = 0;
        }
        
        // Draw Pins
        for (let a of pinsOnCore) {
            let angle = a + coreRotation;
            let startX = CENTER_X + Math.cos(angle) * coreRadius;
            let startY = CENTER_Y + Math.sin(angle) * coreRadius;
            let endX = CENTER_X + Math.cos(angle) * (coreRadius + pinLen);
            let endY = CENTER_Y + Math.sin(angle) * (coreRadius + pinLen);
            
            ctx.beginPath();
            ctx.moveTo(startX, startY);
            ctx.lineTo(endX, endY);
            ctx.strokeStyle = "#fff";
            ctx.lineWidth = 3;
            ctx.stroke();
            
            ctx.beginPath();
            ctx.arc(endX, endY, 5, 0, Math.PI * 2);
            ctx.fillStyle = "#fff";
            ctx.fill();
        }
        
        // Draw Flying Pin
        if (flyingPin) {
            let startX = CENTER_X;
            let startY = flyingPin.y;
            let endX = CENTER_X;
            let endY = flyingPin.y + pinLen;
            
            ctx.beginPath();
            ctx.moveTo(startX, startY);
            ctx.lineTo(endX, endY);
            ctx.strokeStyle = flyingPin.isRisk ? "#ff4757" : "#fff";
            ctx.lineWidth = 3;
            ctx.stroke();
            
            ctx.beginPath();
            ctx.arc(endX, endY, 5, 0, Math.PI * 2);
            ctx.fillStyle = flyingPin.isRisk ? "#ff4757" : "#fff";
            ctx.fill();
        }
    }
}
loop();
