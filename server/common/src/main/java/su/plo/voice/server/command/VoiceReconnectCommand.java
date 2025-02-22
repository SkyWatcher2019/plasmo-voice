package su.plo.voice.server.command;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.lib.api.server.command.MinecraftCommand;
import su.plo.lib.api.server.command.MinecraftCommandSource;
import su.plo.lib.api.server.entity.MinecraftServerPlayer;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.server.BaseVoiceServer;
import su.plo.voice.server.config.ServerLanguage;

@RequiredArgsConstructor
public final class VoiceReconnectCommand implements MinecraftCommand {

    private final BaseVoiceServer voiceServer;

    @Override
    public void execute(@NotNull MinecraftCommandSource source, @NotNull String[] arguments) {
        ServerLanguage language = voiceServer.getLanguages().getLanguage(source);

        if (!(source instanceof MinecraftServerPlayer)) {
            source.sendMessage(language.playerOnlyCommand());
            return;
        }

        MinecraftServerPlayer player = (MinecraftServerPlayer) source;
        VoicePlayer voicePlayer = voiceServer.getPlayerManager().getPlayerById(player.getUUID())
                .orElseThrow(() -> new IllegalStateException("how?"));

        source.sendMessage(language.commands().reconnect().message());
        voiceServer.getUdpConnectionManager().removeConnection(voicePlayer);
        voiceServer.getTcpConnectionManager().connect(voicePlayer);
    }

    @Override
    public boolean hasPermission(@NotNull MinecraftCommandSource source, @Nullable String[] arguments) {
        return source.hasPermission("voice.reconnect");
    }
}
