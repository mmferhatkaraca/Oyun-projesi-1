const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');

let W = window.innerWidth;
let H = window.innerHeight;
canvas.width = W; canvas.height = H;

window.addEventListener('resize', () => {
    W = window.innerWidth; H = window.innerHeight;
    canvas.width = W; canvas.height = H;
});

// Kaynaklar (Assets)
const images = {
    grass: new Image(),
    townhall: new Image(),
    farm: new Image()
};
images.grass.src = 'assets/tile_grass.jpg';
images.townhall.src = 'assets/town_hall.jpg';
images.farm.src = 'assets/farm.jpg';

// Oyun Verileri
let resources = { food: 1000, wood: 500, power: 100 };
const TILE_W = 128;
const TILE_H = 64;
const MAP_SIZE = 15; // 15x15 grid

// Kamera Pan (Sürükleme) Değişkenleri
let camX = W / 2;
let camY = H * 0.2;
let isDragging = false;
let startDragX = 0, startDragY = 0;
let buildMode = null; // null, 'farm', 'townhall'

// Binalar Array'i
let buildings = [
    { type: 'townhall', gridX: 7, gridY: 7, level: 1 }
];

// İzometrik Dönüşüm Fonksiyonları
function getIsoX(gx, gy) {
    return (gx - gy) * (TILE_W / 2);
}
function getIsoY(gx, gy) {
    return (gx + gy) * (TILE_H / 2);
}

// Ekrandan Grid'e Çevirme (İnşaat için)
function getGridFromScreen(screenX, screenY) {
    let adjX = screenX - camX;
    let adjY = screenY - camY;
    let gridX = Math.floor((adjX / (TILE_W / 2) + adjY / (TILE_H / 2)) / 2);
    let gridY = Math.floor((adjY / (TILE_H / 2) - adjX / (TILE_W / 2)) / 2);
    return { x: gridX, y: gridY };
}

// Ana Oyun Döngüsü
function gameLoop() {
    ctx.clearRect(0, 0, W, H);

    // Grid ve Zemin Çizimi
    for (let x = 0; x < MAP_SIZE; x++) {
        for (let y = 0; y < MAP_SIZE; y++) {
            let px = camX + getIsoX(x, y);
            let py = camY + getIsoY(x, y);
            
            // Eğer ekrandan çok uzaksa çizme (Performans optimizasyonu)
            if (px < -200 || px > W + 200 || py < -200 || py > H + 200) continue;
            
            // Zemin Çizimi
            if (images.grass.complete) {
                // Assetler 1024x1024, küçültüp çiziyoruz
                ctx.drawImage(images.grass, px - TILE_W/2, py, TILE_W, TILE_W);
            }
            
            // Grid Çizgileri
            ctx.beginPath();
            ctx.moveTo(px, py);
            ctx.lineTo(px + TILE_W/2, py + TILE_H/2);
            ctx.lineTo(px, py + TILE_H);
            ctx.lineTo(px - TILE_W/2, py + TILE_H/2);
            ctx.closePath();
            ctx.strokeStyle = "rgba(255,255,255,0.1)";
            ctx.stroke();
        }
    }

    // Binaları Çiz (Y eksenine göre sırala ki öndeki arkadakini örtsün)
    buildings.sort((a, b) => (a.gridX + a.gridY) - (b.gridX + b.gridY));
    
    for (let b of buildings) {
        let px = camX + getIsoX(b.gridX, b.gridY);
        let py = camY + getIsoY(b.gridX, b.gridY);
        
        let img = images[b.type];
        if (img && img.complete) {
            // Binayı yukarı doğru offsetle (İzometrik yükselti)
            let drawW = TILE_W * 1.5;
            let drawH = drawW; // Kare oran koruma
            ctx.drawImage(img, px - drawW/2, py - drawH + TILE_H, drawW, drawH);
        }
    }

    requestAnimationFrame(gameLoop);
}

// Ekonomi Döngüsü (Her saniye kaynak üret)
setInterval(() => {
    let farmCount = buildings.filter(b => b.type === 'farm').length;
    resources.food += farmCount * 5; // Her çiftlik saniyede 5 yiyecek üretir
    updateUI();
}, 1000);

function updateUI() {
    document.getElementById('foodAmount').innerText = resources.food;
    document.getElementById('woodAmount').innerText = resources.wood;
    document.getElementById('powerAmount').innerText = resources.power;
}

function showToast(msg) {
    const t = document.getElementById('toast');
    t.innerText = msg;
    t.classList.remove('hidden');
    setTimeout(() => t.classList.add('hidden'), 2000);
}

// İnput İşlemleri (Sürükleme ve Tıklama)
canvas.addEventListener('pointerdown', (e) => {
    isDragging = true;
    startDragX = e.clientX - camX;
    startDragY = e.clientY - camY;
});

canvas.addEventListener('pointermove', (e) => {
    if (isDragging) {
        camX = e.clientX - startDragX;
        camY = e.clientY - startDragY;
    }
});

canvas.addEventListener('pointerup', (e) => {
    isDragging = false;
    
    // Eğer inşa modundaysa
    if (buildMode) {
        let grid = getGridFromScreen(e.clientX, e.clientY);
        
        if (grid.x >= 0 && grid.x < MAP_SIZE && grid.y >= 0 && grid.y < MAP_SIZE) {
            // Bu karede bina var mı?
            let isOccupied = buildings.find(b => b.gridX === grid.x && b.gridY === grid.y);
            if (!isOccupied) {
                if (buildMode === 'farm' && resources.wood >= 100) {
                    resources.wood -= 100;
                    buildings.push({ type: 'farm', gridX: grid.x, gridY: grid.y, level: 1 });
                    resources.power += 50;
                    showToast("Çiftlik İnşa Edildi!");
                    buildMode = null; // Moddan çık
                } else if (buildMode === 'townhall') {
                    showToast("Sadece bir adet Belediye Binası olabilir!");
                    buildMode = null;
                } else {
                    showToast("Yetersiz Kaynak (100 Odun gerekli)!");
                    buildMode = null;
                }
                updateUI();
            } else {
                showToast("Burası Dolu!");
            }
        }
    }
});

// Arayüz Tıklamaları
document.getElementById('btnBuildMenu').onclick = () => {
    document.getElementById('buildMenu').classList.remove('hidden');
};
document.getElementById('btnCloseMenu').onclick = () => {
    document.getElementById('buildMenu').classList.add('hidden');
    buildMode = null;
};
window.selectBuilding = function(type) {
    buildMode = type;
    document.getElementById('buildMenu').classList.add('hidden');
    showToast("İnşa etmek için boş bir alana dokunun.");
};

// Oyunu Başlat
images.grass.onload = () => {
    requestAnimationFrame(gameLoop);
};
