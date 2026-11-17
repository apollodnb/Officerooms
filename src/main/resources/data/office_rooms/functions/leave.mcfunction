# Office Rooms: back to the Overworld.
#
# Step 1: spreadplayers looks for a safe surface around X 0 / Z 0 in the Overworld
# (loads the chunk itself, avoids lava/water).
execute in minecraft:overworld run spreadplayers 0 0 0 16 false @s

# Step 2 (fallback): if the player is still in the Office Rooms, teleport high into the
# Overworld and let them land safely with Slow Falling.
# "at @s" re-reads the player's current dimension.
execute at @s if dimension office_rooms:office_rooms run effect give @s minecraft:slow_falling 30 0 true
execute at @s if dimension office_rooms:office_rooms in minecraft:overworld run tp @s 0.5 250 0.5

tellraw @s {"text":"[Office Rooms] Back in the Overworld.","color":"light_purple"}
