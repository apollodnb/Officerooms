package de.officerooms.hazard;

import java.util.function.Supplier;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** 0.17.0: the mod's network channel. One message: the current hazard event state, sent to all players. */
public final class HazardNet {

    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private HazardNet() {
    }

    public record Sync(int phase, int secondsLeft, int[] box, String name) {
        void encode(FriendlyByteBuf b) {
            b.writeVarInt(phase);
            b.writeVarInt(secondsLeft);
            b.writeVarIntArray(box);
            b.writeUtf(name, 64);
        }

        static Sync decode(FriendlyByteBuf b) {
            return new Sync(b.readVarInt(), b.readVarInt(), b.readVarIntArray(6), b.readUtf(64));
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            HazardState.CLIENT.set(phase, secondsLeft, box, name);
            ctx.get().setPacketHandled(true);
        }
    }

    /** 0.19.0: camera shake (Underside quakes). */
    public record Quake(int ticks, float strength) {
        void encode(FriendlyByteBuf b) {
            b.writeVarInt(ticks);
            b.writeFloat(strength);
        }

        static Quake decode(FriendlyByteBuf b) {
            return new Quake(b.readVarInt(), b.readFloat());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            de.officerooms.underside.UndersideClientState.quake(ticks, strength);
            ctx.get().setPacketHandled(true);
        }
    }

    /** 0.22.0: blackout state. */
    public record Blackout(int phase, int secondsLeft, int[] box, String name, int fuseFloor) {
        void encode(FriendlyByteBuf b) {
            b.writeVarInt(phase);
            b.writeVarInt(secondsLeft);
            b.writeVarIntArray(box);
            b.writeUtf(name);
            b.writeVarInt(fuseFloor);
        }

        static Blackout decode(FriendlyByteBuf b) {
            return new Blackout(b.readVarInt(), b.readVarInt(), b.readVarIntArray(), b.readUtf(), b.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            BlackoutState.CLIENT.set(phase, secondsLeft, box.length == 6 ? box : new int[6], name, fuseFloor);
            ctx.get().setPacketHandled(true);
        }
    }

    /** 0.26.0: open the elevator floor panel. */
    public record ElevatorPanel(net.minecraft.core.BlockPos plate, int storey, int maxStorey) {
        void encode(FriendlyByteBuf b) {
            b.writeBlockPos(plate);
            b.writeVarInt(storey);
            b.writeVarInt(maxStorey);
        }

        static ElevatorPanel decode(FriendlyByteBuf b) {
            return new ElevatorPanel(b.readBlockPos(), b.readVarInt(), b.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            net.neoforged.fml.DistExecutor.unsafeRunWhenOn(net.neoforged.api.distmarker.Dist.CLIENT,
                    () -> () -> de.officerooms.client.ElevatorPanelScreen.open(plate, storey, maxStorey));
            ctx.get().setPacketHandled(true);
        }
    }

    /** 0.26.0: floor typed into the elevator panel. */
    public record ElevatorCall(net.minecraft.core.BlockPos plate, int floor) {
        void encode(FriendlyByteBuf b) {
            b.writeBlockPos(plate);
            b.writeVarInt(floor);
        }

        static ElevatorCall decode(FriendlyByteBuf b) {
            return new ElevatorCall(b.readBlockPos(), b.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> ctx) {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null) {
                de.officerooms.world.ElevatorRides.call(sp, plate, floor);
            }
            ctx.get().setPacketHandled(true);
        }
    }

    public static void register() {
        CHANNEL.messageBuilder(Sync.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Sync::encode).decoder(Sync::decode).consumerMainThread(Sync::handle).add();
        CHANNEL.messageBuilder(Quake.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Quake::encode).decoder(Quake::decode).consumerMainThread(Quake::handle).add();
        CHANNEL.messageBuilder(Blackout.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Blackout::encode).decoder(Blackout::decode).consumerMainThread(Blackout::handle).add();
        CHANNEL.messageBuilder(ElevatorPanel.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ElevatorPanel::encode).decoder(ElevatorPanel::decode).consumerMainThread(ElevatorPanel::handle).add();
        CHANNEL.messageBuilder(ElevatorCall.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ElevatorCall::encode).decoder(ElevatorCall::decode).consumerMainThread(ElevatorCall::handle).add();
    }

    public static void sendAll(Sync s) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), s);
    }

    public static void send(ServerPlayer p, Sync s) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), s);
    }
}
