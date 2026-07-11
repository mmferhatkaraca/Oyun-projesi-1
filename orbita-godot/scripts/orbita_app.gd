extends Node2D

var GameWorld = preload("res://scripts/game_world.gd")
var current_world: Node2D = null
var levels = []
var current_level_idx = 0
var save_data = {"journey_level": 1, "score": 0, "jokers": {"shield": 3, "slow": 3}}
var is_tutorial = false

# UI Nodes
var canvas_layer: CanvasLayer
var menu_ui: Control
var hud_ui: Control
var result_ui: Control
var level_label: Label
var status_label: Label
var btn_shield: Button
var btn_slow: Button

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
    title.text = "ORBITA: FLOW ARENA"
    title.position = Vector2(100, 150)
    title.add_theme_font_size_override("font_size", 24)
    var btn_play = Button.new()
    btn_play.text = "OYNA (Journey)"
    btn_play.position = Vector2(160, 350)
    btn_play.size = Vector2(160, 60)
    btn_play.pressed.connect(start_journey)
    
    var btn_tut = Button.new()
    btn_tut.text = "TUTORIAL"
    btn_tut.position = Vector2(160, 450)
    btn_tut.size = Vector2(160, 60)
    btn_tut.pressed.connect(start_tutorial)
    
    menu_ui.add_child(title)
    menu_ui.add_child(btn_play)
    menu_ui.add_child(btn_tut)
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
    
    btn_shield = Button.new()
    btn_shield.text = "Kalkan(3)"
    btn_shield.position = Vector2(360, 100)
    btn_shield.pressed.connect(use_shield)
    
    btn_slow = Button.new()
    btn_slow.text = "Yavaslat(3)"
    btn_slow.position = Vector2(360, 160)
    btn_slow.pressed.connect(use_slow)
    
    hud_ui.add_child(level_label)
    hud_ui.add_child(status_label)
    hud_ui.add_child(btn_risk)
    hud_ui.add_child(btn_shield)
    hud_ui.add_child(btn_slow)
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
    is_tutorial = false
    menu_ui.hide()
    hud_ui.show()
    update_joker_ui()
    start_level(save_data["journey_level"] - 1)
    
func start_tutorial():
    is_tutorial = true
    menu_ui.hide()
    hud_ui.show()
    update_joker_ui()
    # Mock tutorial level data
    var tut_data = {"id": 0, "name": "Egitim", "speed": 0.5, "reverse_timer": 0, "target_pins": 3, "pulse_speed": 0, "risk_enabled": false}
    launch_world(tut_data)

func update_joker_ui():
    btn_shield.text = "Kalkan (" + str(save_data["jokers"]["shield"]) + ")"
    btn_slow.text = "Yavaslat (" + str(save_data["jokers"]["slow"]) + ")"

func start_level(idx):
    if idx >= levels.size():
        idx = 0 
    current_level_idx = idx
    launch_world(levels[idx])

func launch_world(level_data):
    if current_world:
        current_world.queue_free()
    
    current_world = GameWorld.new()
    current_world.setup(level_data)
    current_world.connect("game_over", _on_game_over)
    current_world.connect("level_cleared", _on_level_cleared)
    current_world.connect("status_updated", _on_status_updated)
    add_child(current_world)
    
    level_label.text = "Level " + str(level_data["id"]) + " : " + level_data["name"]
    _on_status_updated("Dokun ve Firlat!")

func toggle_risk():
    if current_world:
        current_world.toggle_risk()

func use_shield():
    if current_world and save_data["jokers"]["shield"] > 0:
        save_data["jokers"]["shield"] -= 1
        current_world.activate_shield()
        update_joker_ui()

func use_slow():
    if current_world and save_data["jokers"]["slow"] > 0:
        save_data["jokers"]["slow"] -= 1
        current_world.activate_slow()
        update_joker_ui()

func _on_status_updated(msg):
    status_label.text = msg

func _on_level_cleared():
    hud_ui.hide()
    result_ui.show()
    result_ui.get_node("Title").text = "LEVEL GECILDI!"
    result_ui.get_node("Title").add_theme_color_override("font_color", Color.GREEN)
    if not is_tutorial:
        save_data["journey_level"] = current_level_idx + 2

func _on_game_over():
    hud_ui.hide()
    result_ui.show()
    result_ui.get_node("Title").text = "CARPISMA!"
    result_ui.get_node("Title").add_theme_color_override("font_color", Color.RED)
