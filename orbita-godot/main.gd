extends Node2D

const TILE_W = 120
const TILE_H = 60
const MAP_SIZE = 15

var buildings = []
var resources = {"food": 500, "wood": 1000}

var dragging = false
var drag_start = Vector2()
var cam_start = Vector2()

@onready var camera = $Camera2D
@onready var ui_food = $UI/TopBar/HBox/FoodLbl
@onready var ui_wood = $UI/TopBar/HBox/WoodLbl
@onready var ui_toast = $UI/Toast

var build_mode = ""

func _ready():
    # Başlangıç Binası (Belediye)
    buildings.append({"type": "townhall", "x": 7, "y": 7})
    camera.position = iso_to_screen(7, 7)
    
    $UI/BottomBar/BtnFarm.pressed.connect(_on_btn_farm)
    
    # 1 saniyelik ekonomi döngüsü
    var timer = Timer.new()
    timer.wait_time = 1.0
    timer.autostart = true
    timer.timeout.connect(_on_tick)
    add_child(timer)
    update_ui()

func _on_tick():
    var farms = 0
    for b in buildings:
        if b.type == "farm": farms += 1
    resources.food += farms * 10
    update_ui()

func update_ui():
    ui_food.text = "🌾 " + str(resources.food)
    ui_wood.text = "🪵 " + str(resources.wood)

func iso_to_screen(x, y):
    return Vector2((x - y) * TILE_W, (x + y) * TILE_H)

func screen_to_iso(pos: Vector2):
    var adj = pos
    var map_x = (adj.x / float(TILE_W) + adj.y / float(TILE_H)) / 2.0
    var map_y = (adj.y / float(TILE_H) - adj.x / float(TILE_W)) / 2.0
    return Vector2(floor(map_x), floor(map_y))

func _unhandled_input(event):
    if event is InputEventScreenTouch or event is InputEventMouseButton:
        if event.pressed:
            dragging = true
            drag_start = event.position
            cam_start = camera.position
        else:
            dragging = false
            if event.position.distance_to(drag_start) < 15:
                handle_click(get_global_mouse_position())
                
    elif event is InputEventScreenDrag or event is InputEventMouseMotion:
        if dragging:
            var diff = drag_start - event.position
            camera.position = cam_start + diff

func handle_click(gpos):
    if build_mode == "": return
    var grid = screen_to_iso(gpos)
    
    if grid.x >= 0 and grid.x < MAP_SIZE and grid.y >= 0 and grid.y < MAP_SIZE:
        var empty = true
        for b in buildings:
            if b.x == grid.x and b.y == grid.y: empty = false
        
        if empty:
            if build_mode == "farm" and resources.wood >= 100:
                resources.wood -= 100
                buildings.append({"type": "farm", "x": grid.x, "y": grid.y})
                show_toast("Çiftlik Kuruldu!")
                build_mode = ""
                update_ui()
            else:
                show_toast("Yetersiz Kaynak!")
        else:
            show_toast("Zemin Dolu!")
    queue_redraw()

func show_toast(msg):
    ui_toast.text = msg
    ui_toast.show()
    await get_tree().create_timer(1.5).timeout
    ui_toast.hide()

func _on_btn_farm():
    build_mode = "farm"
    show_toast("Haritaya dokun")

func _process(_delta):
    queue_redraw()

func _draw():
    # Çimen Zemin (Isometric)
    for x in range(MAP_SIZE):
        for y in range(MAP_SIZE):
            var p = iso_to_screen(x, y)
            var pts = PackedVector2Array([
                p + Vector2(0, -TILE_H),
                p + Vector2(TILE_W, 0),
                p + Vector2(0, TILE_H),
                p + Vector2(-TILE_W, 0)
            ])
            # Satranç tahtası gibi iki tonlu yeşil
            var c = Color(0.2, 0.5, 0.2) if int(x+y) % 2 == 0 else Color(0.25, 0.55, 0.25)
            draw_colored_polygon(pts, c)
            # Grid sınır çizgileri
            draw_polyline(PackedVector2Array([pts[0], pts[1], pts[2], pts[3], pts[0]]), Color(0,0,0, 0.15), 2.0)

    # Binaları Y eksenine göre sırala (Öndeki arkadakini kapatsın diye)
    var sorted_b = buildings.duplicate()
    sorted_b.sort_custom(func(a, b): return (a.x + a.y) < (b.x + b.y))

    for b in sorted_b:
        var p = iso_to_screen(b.x, b.y)
        if b.type == "townhall":
            draw_townhall(p)
        elif b.type == "farm":
            draw_farm(p)

func draw_townhall(p):
    var h = 90
    # Sol Duvar (Gölge)
    draw_colored_polygon(PackedVector2Array([p, p+Vector2(-TILE_W*0.7, -TILE_H*0.7), p+Vector2(-TILE_W*0.7, -TILE_H*0.7 - h), p+Vector2(0, -h)]), Color(0.4, 0.4, 0.45))
    # Sağ Duvar (Işık)
    draw_colored_polygon(PackedVector2Array([p, p+Vector2(TILE_W*0.7, -TILE_H*0.7), p+Vector2(TILE_W*0.7, -TILE_H*0.7 - h), p+Vector2(0, -h)]), Color(0.6, 0.6, 0.65))
    
    # Çatı (Mavi Piramit)
    var roof_top = p + Vector2(0, -h - 80)
    draw_colored_polygon(PackedVector2Array([p+Vector2(-TILE_W*0.7, -TILE_H*0.7 - h), p+Vector2(0, -h), roof_top]), Color(0.2, 0.4, 0.7))
    draw_colored_polygon(PackedVector2Array([p+Vector2(TILE_W*0.7, -TILE_H*0.7 - h), p+Vector2(0, -h), roof_top]), Color(0.3, 0.5, 0.8))

func draw_farm(p):
    # Çiftlik Zemini (Toprak)
    var pts = PackedVector2Array([
        p + Vector2(0, -TILE_H*0.8),
        p + Vector2(TILE_W*0.8, 0),
        p + Vector2(0, TILE_H*0.8),
        p + Vector2(-TILE_W*0.8, 0)
    ])
    draw_colored_polygon(pts, Color(0.4, 0.25, 0.15)) 
    
    # Ekinler (Sarı)
    var cw = TILE_W * 0.4
    var ch = TILE_H * 0.4
    for i in range(3):
        var cp = p + Vector2(0, -15 + i*15)
        var crop_pts = PackedVector2Array([cp, cp+Vector2(cw, -ch), cp+Vector2(0, -ch*2), cp+Vector2(-cw, -ch)])
        draw_colored_polygon(crop_pts, Color(0.9, 0.8, 0.2))
