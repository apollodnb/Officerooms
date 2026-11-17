# Test report 0.21.2

- Build OK; unit tests pass.
- The test environment has Refurbished Furniture installed. All six block ids used exist: `oak_chair`, `oak_table`, `yellow_sofa`, `light_gray_sofa`, `oak_drawer`, `oak_storage_cabinet`.
- New area of Level 0 (around 20000/20000), 16 views: Refurbished chair + table, a grey sofa and a yellow sofa were seen (screenshot). No exceptions.
- Not tested: running without Refurbished. The fallback path keeps the vanilla blocks; it is in the code but was not run.
- Not tested: sitting on the chairs in Level 0.
