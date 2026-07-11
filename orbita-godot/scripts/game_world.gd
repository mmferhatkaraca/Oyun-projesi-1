extends Node2D

signal game_over
signal level_cleared
signal status_updated(msg)

var center = Vector2(240, 300)
var core_radius = 50.0
var pin_length = 40.0
var pin_speed = 1500.0

var level_data = {}
var current_rotation = 0.0
var pulse_angle = 0.0
var reverse_timer_current = 0.0

var pins_on_core = [] # Array of angles (float)
var flying_pin = null # Dictionary: {y, is_risk}
var pins_left = 0

var state = "playing" # playing, cleared, failed
var risk_active = false
var combo = 0

func setup(data):
    level_data = data
    pins_left = data["target_pins"]
    pulse_angle = 0.0
    
func _process(delta):
    if state != "playing": return
    
    # Update Core Rotation
    var current_speed = level_data["speed"]
    if level_data["reverse_timer"] > 0:
        reverse_timer_current += delta
        if reverse_timer_current > level_data["reverse_timer"]:
            level_data["speed"] = -level_data["speed"]
            reverse_timer_current = 0.0
            status_updated.emit("YON DEGISIYOR!")
            
    current_rotation = fmod(current_rotation + current_speed * delta, PI * 2)
    pulse_angle = fmod(pulse_angle + level_data["pulse_speed"] * delta, PI * 2)
    
    # Update Flying Pin
    if flying_pin != null:
        flying_pin["y"] -= pin_speed * delta
        var target_y = center.y + core_radius
        if flying_pin["y"] <= target_y:
            _attach_pin()
            
    queue_redraw()

func _input(event):
    if state == "playing" and event is InputEventScreenTouch and event.pressed and flying_pin == null:
        if pins_left > 0:
            shoot_pin()

func toggle_risk():
    if level_data.get("risk_enabled", false):
        risk_active = !risk_active
        status_updated.emit("RISK MODU: " + ("ACIK" if risk_active else "KAPALI"))

func shoot_pin():
    pins_left -= 1
    flying_pin = {"y": 700.0, "is_risk": risk_active}
    status_updated.emit("Kalan Igne: " + str(pins_left))

func _attach_pin():
    var attach_angle = fmod(-current_rotation + PI*2, PI*2)
    
    # Collision Logic
    var safe_dist = 0.25 # radians (~14 degrees)
    if flying_pin["is_risk"]: safe_dist = 0.4 # Shrink safe zone in risk mode
    
    var collided = false
    for p in pins_on_core:
        var diff = abs(p - attach_angle)
        if diff > PI: diff = PI*2 - diff
        if diff < safe_dist:
            collided = true
            break
            
    if collided:
        state = "failed"
        game_over.emit()
        flying_pin = null
        queue_redraw()
        return
        
    # Pulse Logic
    var pulse_diff = abs(attach_angle - fmod(-pulse_angle + PI*2, PI*2))
    if pulse_diff > PI: pulse_diff = PI*2 - pulse_diff
    if pulse_diff < 0.3:
        combo += 1
        status_updated.emit("PERFECT PULSE! Combo x" + str(combo))
    else:
        combo = 0
        
    pins_on_core.append(attach_angle)
    flying_pin = null
    
    if pins_left <= 0:
        state = "cleared"
        level_cleared.emit()

func _draw():
    # Background
    draw_rect(Rect2(0,0, 480, 854), Color(0.05, 0.05, 0.15))
    
    # Draw Chain Lines (Anatolian Geometry)
    if pins_on_core.size() >= 3:
        var p_points = []
        for a in pins_on_core:
            var end_x = center.x + cos(a + current_rotation) * (core_radius + pin_length)
            var end_y = center.y + sin(a + current_rotation) * (core_radius + pin_length)
            p_points.append(Vector2(end_x, end_y))
        for i in range(p_points.size() - 1):
            draw_line(p_points[i], p_points[i+1], Color(0, 0.8, 0.8, 0.3), 2.0)
    
    # Draw Core
    var core_color = Color(0.8, 0.2, 0.2) if risk_active else Color(0.0, 0.8, 0.8)
    draw_circle(center, core_radius, core_color)
    
    # Draw Pulse Window Arc (Amber color)
    if level_data.get("pulse_speed", 0) > 0 or true:
        var p_x = center.x + cos(pulse_angle) * core_radius
        var p_y = center.y + sin(pulse_angle) * core_radius
        draw_circle(Vector2(p_x, p_y), 8, Color(1, 0.7, 0)) # Amber target
        
    # Draw Attached Pins
    for a in pins_on_core:
        var end_x = center.x + cos(a + current_rotation) * (core_radius + pin_length)
        var end_y = center.y + sin(a + current_rotation) * (core_radius + pin_length)
        var start_x = center.x + cos(a + current_rotation) * core_radius
        var start_y = center.y + sin(a + current_rotation) * core_radius
        draw_line(Vector2(start_x, start_y), Vector2(end_x, end_y), Color.WHITE, 3.0)
        draw_circle(Vector2(end_x, end_y), 5, Color.WHITE)
        
    # Draw Flying Pin
    if flying_pin != null:
        var fly_c = Color.RED if flying_pin["is_risk"] else Color.WHITE
        draw_line(Vector2(center.x, flying_pin["y"]), Vector2(center.x, flying_pin["y"] + pin_length), fly_c, 3.0)
        draw_circle(Vector2(center.x, flying_pin["y"] + pin_length), 5, fly_c)
