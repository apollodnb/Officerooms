# Test report 0.39.1

Build and all unit tests pass.

In-game test: production server with all test mods, dev client.

| Check | Result |
|---|---|
| Forge mod list: logo, version, credits, author, license, description | ok (photographed) |
| Creatures still render after removing the old code models: dread and friendly wolfspider (code models), gaunt, colony, headless warden, observer, hunter, hound (GeckoLib) | ok, no missing textures, no errors in the client log |
| Compiler warnings | 100 deprecation warnings; all are vanilla block methods meant to be overridden in 1.20.1 or APIs that only change in 1.21 (`new ResourceLocation`, `FMLJavaModLoadingContext.get()`). Left as they are; relevant for a port to 1.21. |

Not tested:
- the logo on a high-resolution screen;
- multiplayer, sound.
