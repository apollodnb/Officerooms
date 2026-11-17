# Office Rooms 0.6.3 – OfficeCraft furniture with shader packs (experimental)

Works with 0.6.1/0.6.2 worlds.

## The problem

OfficeCraft draws its furniture with its own vertex buffers and its own shader, outside Minecraft's normal render types. Shader loaders (Oculus/Iris) replace Minecraft's rendering with the shader pack and never see those draws, so OfficeCraft furniture is invisible (only the block outlines remain).

## What Office Rooms does now

Office Rooms puts a thin wrapper around OfficeCraft's furniture renderer:

- **No shader pack active:** OfficeCraft draws exactly as before (unchanged).
- **Shader pack active (Oculus/Iris):** Office Rooms reads the same furniture meshes from your installed OfficeCraft JAR and draws them through Minecraft's normal entity render types, which the shader pack processes. Desks, chairs, monitors (on/off), lamps (lit/unlit), glass and water bottles (translucent), and props on desks are supported.

Nothing from OfficeCraft is included in Office Rooms; the meshes and textures are read at runtime from OfficeCraft itself. Tested against OfficeCraft 1.0.3.

## Setting (client config `office_rooms-client.toml`, section `[compat]`)

| `officeCraftShaderCompat` | Effect |
|---|---|
| `"AUTO"` (default) | shader-friendly drawing only while a shader pack is active |
| `"ALWAYS"` | always use the shader-friendly drawing |
| `"OFF"` | always OfficeCraft's own drawing |

The file is reloaded while the game runs. If anything goes wrong while drawing, Office Rooms logs an error and falls back to OfficeCraft's own renderer.

## Notes

- Lighting comes from the shader pack (or Minecraft's entity lighting), so furniture looks slightly different from OfficeCraft's own flat lighting.
- Furniture held in the hand or shown as an item still uses OfficeCraft's item renderer.
- Many furniture pieces are drawn every frame in both modes; with shaders, a lower render distance in the Office Rooms helps FPS.
