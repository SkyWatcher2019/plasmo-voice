package su.plo.lib.mod.server.entity;

import com.google.common.collect.Sets;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.lib.api.chat.MinecraftTextConverter;
import su.plo.lib.api.server.MinecraftServerLib;
import su.plo.lib.api.server.entity.MinecraftServerPlayer;
import su.plo.lib.api.server.permission.PermissionTristate;
import su.plo.lib.api.server.world.MinecraftServerWorld;
import su.plo.lib.api.server.world.ServerPos3d;
import su.plo.lib.mod.client.texture.ResourceCache;
import su.plo.lib.mod.entity.ModPlayer;
import su.plo.voice.server.player.PermissionSupplier;

import java.util.Collection;
import java.util.Set;

public final class ModServerPlayer extends ModPlayer<ServerPlayer> implements MinecraftServerPlayer {

    private final MinecraftServerLib minecraftServer;
    private final MinecraftTextConverter<Component> textConverter;
    private final PermissionSupplier permissions;
    private final ResourceCache resources;
    private final Set<String> registeredChannels = Sets.newCopyOnWriteArraySet();
    @Getter
    @Setter
    private String language = "en_us";

    public ModServerPlayer(@NotNull MinecraftServerLib minecraftServer,
                           @NotNull MinecraftTextConverter<Component> textConverter,
                           @NotNull PermissionSupplier permissions,
                           @NotNull ResourceCache resources,
                           @NotNull ServerPlayer player) {
        super(player);

        this.minecraftServer = minecraftServer;
        this.textConverter = textConverter;
        this.permissions = permissions;
        this.resources = resources;
    }

    @Override
    public @NotNull ServerPos3d getServerPosition() {
        return new ServerPos3d(
                minecraftServer.getWorld(instance.getLevel()),
                instance.position().x(),
                instance.position().y(),
                instance.position().z(),
                instance.getXRot(),
                instance.getYRot()
        );
    }

    @Override
    public @NotNull ServerPos3d getServerPosition(@NotNull ServerPos3d position) {
        position.setWorld(minecraftServer.getWorld(instance.getLevel()));

        position.setX(instance.position().x());
        position.setY(instance.position().y());
        position.setZ(instance.position().z());

        position.setYaw(instance.getXRot());
        position.setPitch(instance.getYRot());

        return position;
    }

    @Override
    public @NotNull MinecraftServerWorld getWorld() {
        return minecraftServer.getWorld(instance.getLevel());
    }

    @Override
    public void sendPacket(@NotNull String channel, byte[] data) {
        instance.connection.send(new ClientboundCustomPayloadPacket(
                resources.getLocation(channel),
                new FriendlyByteBuf(Unpooled.wrappedBuffer(data))
        ));
    }

    @Override
    public void kick(@NotNull MinecraftTextComponent reason) {
        instance.connection.disconnect(textConverter.convert(reason));
    }

    @Override
    public void sendMessage(@NotNull MinecraftTextComponent text) {
        instance.sendSystemMessage(textConverter.convert(text));
    }

    @Override
    public void sendMessage(@NotNull String text) {
        instance.sendSystemMessage(Component.literal(text));
    }

    @Override
    public boolean canSee(@NotNull MinecraftServerPlayer player) {
        return !player.isInvisibleTo(player);
    }

    @Override
    public Collection<String> getRegisteredChannels() {
        return registeredChannels;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return permissions.hasPermission(instance, permission);
    }

    @Override
    public @NotNull PermissionTristate getPermission(@NotNull String permission) {
        return permissions.getPermission(instance, permission);
    }

    public void addChannel(@NotNull String channel) {
        registeredChannels.add(channel);
    }

    public void removeChannel(@NotNull String channel) {
        registeredChannels.remove(channel);
    }
}
