extends Node2D

var GameWorld = preload("res://scripts/game_world.gd")
var current_world: Node2D = null
var levels = []
var current_level_idx = 0
var save_data = {"journey_level": 1, "score": 0, "jokers": {"shield": 1, "slow": 1}}

# UI Nodes
var canvas_layer: CanvasLayer
var menu_ui: Control
var hud_ui: Control
var result_ui: Control
var level_label: Label
var status_label: Label

func _ready():
    load_levels()
    setup_ui()
    show_menu()

func load_levels():
    var file = FileAccess.open("res://data/levels.json", FileAccess.READ)
    if file:
        var text = file.get_as_text()
        var json = JSON.new()
        if json.parse(text) == OK:
            levels = json.data
    if levels.is_empty():
        print("MOCK LEVEL YUKLENDI - Dosya hatasi")
        levels = [{"id": 1, "name": "Fallback", "speed": 1.0, "reverse_timer": 0, "target_pins": 5, "pulse_speed": 0, "risk_enabled": false}]

func setup_ui():
    canvas_layer = CanvasLayer.new()
    add_child(canvas_layer)
    
    # Menu
    menu_ui = Control.new()
    menu_ui.set_anchors_preset(Control.PRESET_FULL_RECT)
    var title = Label.new()
    title.text = "ORBITA: FLOW ARENA\n(Anadolu Bilimkurgu)"
    title.position = Vector2(100, 150)
    title.add_theme_font_size_override("font_size", 24)
    var btn_play = Button.new()
    btn_play.text = "OYNA (Journey)"
    btn_play.position = Vector2(160, 400)
    btn_play.size = Vector2(160, 60)
    btn_play.pressed.connect(start_journey)
    menu_ui.add_child(title)
    menu_ui.add_child(btn_play)
    canvas_layer.add_child(menu_ui)
    
    # HUD
    hud_ui = Control.new()
    hud_ui.set_anchors_preset(Control.PRESET_FULL_RECT)
    level_label = Label.new()
    level_label.position = Vector2(20, 40)
    level_label.add_theme_font_size_override("font_size", 20)
    status_label = Label.new()
    status_label.position = Vector2(20, 80)
    status_label.add_theme_color_override("font_color", Color(0, 0.8, 0.8))
    var btn_risk = Button.new()
    btn_risk.text = "RISK x2"
    btn_risk.position = Vector2(360, 40)
    btn_risk.pressed.connect(toggle_risk)
    var btn_joker = Button.new()
    btn_joker.text = "JOKER (Kalkan)"
    btn_joker.position = Vector2(340, 80)
    hud_ui.add_child(level_label)
    hud_ui.add_child(status_label)
    hud_ui.add_child(btn_risk)
    hud_ui.add_child(btn_joker)
    hud_ui.hide()
    canvas_layer.add_child(hud_ui)
    
    # Result
    result_ui = Control.new()
    result_ui.set_anchors_preset(Control.PRESET_FULL_RECT)
    var res_title = Label.new()
    res_title.name = "Title"
    res_title.position = Vector2(160, 200)
    res_title.add_theme_font_size_override("font_size", 30)
    var btn_next = Button.new()
    btn_next.text = "DEVAM ET"
    btn_next.position = Vector2(160, 400)
    btn_next.size = Vector2(160, 60)
    btn_next.pressed.connect(show_menu)
    result_ui.add_child(res_title)
    result_ui.add_child(btn_next)
    result_ui.hide()
    canvas_layer.add_child(result_ui)

func show_menu():
    menu_ui.show()
    hud_ui.hide()
    result_ui.hide()
    if current_world:
        current_world.queue_free()
        current_world = null

func start_journey():
    menu_ui.hide()
    hud_ui.show()
    start_level(save_data["journey_level"] - 1)

func start_level(idx):
    if idx >= levels.size():
        idx = 0 # Loop for prototype
    current_level_idx = idx
    var level_data = levels[idx]
    
    if current_world:
        current_world.queue_free()
    
    current_world = GameWorld.new()
    current_world.setup(level_data)
    current_world.connect("game_over", _on_game_over)
    current_world.connect("level_cleared", _on_level_cleared)
    current_world.connect("status_updated", _on_status_updated)
    add_child(current_world)
    
    level_label.text = "Level " + str(level_data["id"]) + " : " + level_data["name"]
    _on_status_updated("Hazir!")

func toggle_risk():
    if current_world:
        current_world.toggle_risk()

func _on_status_updated(msg):
    status_label.text = msg

func _on_level_cleared():
    hud_ui.hide()
    result_ui.show()
    result_ui.get_node("Title").text = "LEVEL GECILDI!"
    result_ui.get_node("Title").add_theme_color_override("font_color", Color.GREEN)
    save_data["journey_level"] = current_level_idx + 2

func _on_game_over():
    hud_ui.hide()
    result_ui.show()
    result_ui.get_node("Title").text = "CARPISMA!"
    result_ui.get_node("Title").add_theme_color_override("font_color", Color.RED)
