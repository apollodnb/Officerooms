package de.officerooms.worldgen;

import com.mojang.serialization.Codec;
import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registriert den Codec des Office-Rooms-Chunk-Generators unter {@code office_rooms:office_rooms}.
 * Die Dimension verweist in {@code dimension/office_rooms.json} per {@code "type"} darauf.
 */
public final class OfficeWorldgen {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, OfficeRoomsIds.MOD_ID);

    public static final RegistryObject<Codec<OfficeChunkGenerator>> OFFICE =
            CHUNK_GENERATORS.register("office_rooms", () -> OfficeChunkGenerator.CODEC);
    static {
        CHUNK_GENERATORS.register("underside", () -> de.officerooms.underside.UndersideChunkGenerator.CODEC);
        CHUNK_GENERATORS.register("level_0", () -> de.officerooms.level0.Level0ChunkGenerator.CODEC);
    }

    private OfficeWorldgen() {
    }

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
    }
}
