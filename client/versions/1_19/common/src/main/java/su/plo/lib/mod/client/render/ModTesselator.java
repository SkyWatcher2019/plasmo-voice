package su.plo.lib.mod.client.render;

import com.mojang.blaze3d.vertex.Tesselator;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.client.render.MinecraftTesselator;
import su.plo.lib.api.client.render.VertexBuilder;

public final class ModTesselator implements MinecraftTesselator {

    private VertexBuilder builder;

    @Override
    public @NotNull VertexBuilder getBuilder() {
        if (builder == null) {
            this.builder = new ModVertexBuilder(Tesselator.getInstance().getBuilder());
        }

        return builder;
    }

    @Override
    public void end() {
        Tesselator.getInstance().end();
    }
}
