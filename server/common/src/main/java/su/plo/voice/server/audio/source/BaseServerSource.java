package su.plo.voice.server.audio.source;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.voice.api.addon.AddonContainer;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerAudioSource;
import su.plo.voice.api.server.connection.UdpServerConnectionManager;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.pos.ServerPos3d;
import su.plo.voice.api.server.socket.UdpConnection;
import su.plo.voice.proto.packets.Packet;
import su.plo.voice.proto.packets.udp.cllientbound.SourceAudioPacket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public abstract class BaseServerSource implements ServerAudioSource {

    protected final UdpServerConnectionManager udpConnections;
    @Getter
    protected final AddonContainer addon;
    @Getter
    protected final UUID id;
    protected final String codec;
    protected final boolean stereo;

    @Getter
    @Setter
    protected @NotNull ServerSourceLine line;
    @Getter
    @Setter
    protected boolean iconVisible = true;
    @Setter
    protected int angle;

    protected final AtomicInteger state = new AtomicInteger(1);

    private final List<Predicate<VoicePlayer>> filters = new CopyOnWriteArrayList<>();
    private final ServerPos3d playerPosition = new ServerPos3d();

    public BaseServerSource(@NotNull UdpServerConnectionManager udpConnections,
                            @NotNull AddonContainer addon,
                            @NotNull UUID id,
                            @NotNull ServerSourceLine line,
                            @Nullable String codec,
                            boolean stereo) {
        this.udpConnections = udpConnections;
        this.addon = addon;
        this.id = id;
        this.line = line;
        this.codec = codec;
        this.stereo = stereo;
    }

    @Override
    public int getState() {
        return state.get();
    }

    @Override
    public void addFilter(Predicate<VoicePlayer> filter) {
        if (filters.contains(filter)) throw new IllegalArgumentException("Filter already exist");
        filters.add(filter);
    }

    @Override
    public void removeFilter(Predicate<VoicePlayer> filter) {
        filters.remove(filter);
    }

    @Override
    public void sendAudioPacket(SourceAudioPacket packet, short distance) {
        distance *= 2;

        ServerPos3d sourcePosition = getPosition();
        double distanceSquared = distance * distance;

        L:
        for (UdpConnection connection : udpConnections.getConnections()) {
            for (Predicate<VoicePlayer> filter : filters) {
                if (!filter.test(connection.getPlayer())) continue L;
            }

            connection.getPlayer().getPosition(playerPosition);
            if (sourcePosition.distanceSquared(playerPosition) <= distanceSquared) {
                connection.sendPacket(packet);
            }
        }
    }

    @Override
    public void sendPacket(Packet<?> packet, short distance) {
        distance *= 2;

        ServerPos3d sourcePosition = getPosition();
        double distanceSquared = distance * distance;

        L:
        for (UdpConnection connection : udpConnections.getConnections()) {
            for (Predicate<VoicePlayer> filter : filters) {
                if (!filter.test(connection.getPlayer())) continue L;
            }

            connection.getPlayer().getPosition(playerPosition);
            if (sourcePosition.distanceSquared(playerPosition) <= distanceSquared) {
                connection.getPlayer().sendPacket(packet);
            }
        }
    }

    protected void incrementState() {
        state.updateAndGet((operand) -> {
            int value = operand + 1;
            return value > Byte.MAX_VALUE ? Byte.MIN_VALUE : value;
        });
    }
}
