"""Mechanical part of porting Java sources from Forge 1.20.1 to NeoForge 1.21.1 (package and simple API renames).
The rest is done by hand. Idempotent. python3 tools/port_java_1211.py [files...]"""
import os, re, sys

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src')
PAIRS = [
    ('net.minecraftforge.eventbus.api.', 'net.neoforged.bus.api.'),
    ('net.minecraftforge.common.MinecraftForge', 'net.neoforged.neoforge.common.NeoForge'),
    ('MinecraftForge.EVENT_BUS', 'NeoForge.EVENT_BUS'),
    ('net.minecraftforge.common.ForgeConfigSpec', 'net.neoforged.neoforge.common.ModConfigSpec'),
    ('ForgeConfigSpec', 'ModConfigSpec'),
    ('net.minecraftforge.client.event.', 'net.neoforged.neoforge.client.event.'),
    ('net.minecraftforge.event.', 'net.neoforged.neoforge.event.'),
    ('net.minecraftforge.registries.RegistryObject', 'net.neoforged.neoforge.registries.DeferredHolder'),
    ('net.minecraftforge.registries.DeferredRegister', 'net.neoforged.neoforge.registries.DeferredRegister'),
    ('net.minecraftforge.api.distmarker.', 'net.neoforged.api.distmarker.'),
    ('net.minecraftforge.fml.common.Mod', 'net.neoforged.fml.common.Mod'),
    ('net.minecraftforge.fml.ModList', 'net.neoforged.fml.ModList'),
    ('net.minecraftforge.fml.loading.FMLPaths', 'net.neoforged.fml.loading.FMLPaths'),
    ('net.minecraftforge.fml.event.lifecycle.', 'net.neoforged.fml.event.lifecycle.'),
    ('net.minecraftforge.fml.util.ObfuscationReflectionHelper', 'net.neoforged.fml.util.ObfuscationReflectionHelper'),
    ('net.minecraftforge.entity.PartEntity', 'net.neoforged.neoforge.entity.PartEntity'),
    ('net.minecraftforge.common.ForgeSpawnEggItem', 'net.neoforged.neoforge.common.DeferredSpawnEggItem'),
    ('ForgeSpawnEggItem', 'DeferredSpawnEggItem'),
    ('net.minecraftforge.fml.config.ModConfig', 'net.neoforged.fml.config.ModConfig'),
    ('net.minecraftforge.fml.', 'net.neoforged.fml.'),
    ('net.minecraftforge.client.', 'net.neoforged.neoforge.client.'),
    ('net.minecraftforge.common.', 'net.neoforged.neoforge.common.'),
]
RO = [  # RegistryObject<X> -> DeferredHolder<R, X>
    (r'RegistryObject<Block>', 'DeferredHolder<Block, Block>'),
    (r'RegistryObject<Item>', 'DeferredHolder<Item, Item>'),
    (r'RegistryObject<SoundEvent>', 'DeferredHolder<SoundEvent, SoundEvent>'),
    (r'RegistryObject<EntityType<([A-Za-z0-9_.<>?]+)>>', r'DeferredHolder<EntityType<?>, EntityType<\1>>'),
    (r'RegistryObject<BlockEntityType<([A-Za-z0-9_.<>?]+)>>', r'DeferredHolder<BlockEntityType<?>, BlockEntityType<\1>>'),
    (r'RegistryObject<CreativeModeTab>', 'DeferredHolder<CreativeModeTab, CreativeModeTab>'),
    (r'RegistryObject<MobEffect>', 'DeferredHolder<MobEffect, MobEffect>'),
    (r'RegistryObject<([A-Za-z0-9_.]+)>', r'DeferredHolder<\1, \1>'),
    (r'\bRegistryObject\b', 'DeferredHolder'),
]


def port(text):
    for a, b in PAIRS:
        text = text.replace(a, b)
    for a, b in RO:
        text = re.sub(a, b, text)
    text = re.sub(r'new ResourceLocation\(([^(),]+(?:\([^()]*\))?),\s*', r'ResourceLocation.fromNamespaceAndPath(\1, ', text)
    text = re.sub(r'new ResourceLocation\(', 'ResourceLocation.parse(', text)
    return text


files = sys.argv[1:] or [os.path.join(d, f) for d, _, fs in os.walk(SRC) for f in fs if f.endswith('.java')]
changed = 0
for p in files:
    s = open(p).read()
    t = port(s)
    if t != s:
        open(p, 'w').write(t); changed += 1
print('ported', changed, 'files')
