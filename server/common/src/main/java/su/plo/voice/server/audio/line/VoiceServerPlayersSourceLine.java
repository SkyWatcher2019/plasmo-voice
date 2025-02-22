package su.plo.voice.server.audio.line;

import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.addon.AddonContainer;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.line.ServerPlayersSourceLine;
import su.plo.voice.proto.packets.tcp.clientbound.SourceLinePlayerAddPacket;
import su.plo.voice.proto.packets.tcp.clientbound.SourceLinePlayerRemovePacket;
import su.plo.voice.proto.packets.tcp.clientbound.SourceLinePlayersClearPacket;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

@ToString(callSuper = true)
public final class VoiceServerPlayersSourceLine extends VoiceServerSourceLine implements ServerPlayersSourceLine {

    public VoiceServerPlayersSourceLine(@NotNull PlasmoVoiceServer voiceServer,
                                        @NotNull AddonContainer addon,
                                        @NotNull String name,
                                        @NotNull String translation,
                                        @NotNull String icon,
                                        int weight) {
        super(voiceServer, addon, name, translation, icon, weight);

        this.players = new CopyOnWriteArraySet<>();
    }

    @Override
    public void addPlayer(@NotNull UUID playerId) {
        players.add(playerId);

        voiceServer.getTcpConnectionManager().broadcast(
                new SourceLinePlayerAddPacket(id, playerId),
                null
        );
    }

    @Override
    public boolean removePlayer(@NotNull UUID playerId) {
        if (players.remove(playerId)) {
            voiceServer.getTcpConnectionManager().broadcast(
                    new SourceLinePlayerRemovePacket(id, playerId),
                    null
            );

            return true;
        }

        return false;
    }

    @Override
    public void clearPlayers() {
        players.clear();
        voiceServer.getTcpConnectionManager().broadcast(
                new SourceLinePlayersClearPacket(id),
                null
        );
    }
}
