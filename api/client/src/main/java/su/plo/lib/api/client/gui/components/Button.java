package su.plo.lib.api.client.gui.components;

import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.chat.MinecraftTextComponent;
import su.plo.lib.api.client.MinecraftClientLib;
import su.plo.lib.api.client.gui.GuiRender;
import su.plo.lib.api.client.gui.narration.NarrationOutput;

import java.util.function.Consumer;

public class Button extends AbstractButton {

    public static final OnTooltip NO_TOOLTIP = (button, render, mouseX, mouseY) -> {};
    public static final OnPress NO_ACTION = (button) -> {};

    protected final OnPress pressAction;
    protected final OnTooltip tooltipAction;

    public Button(@NotNull MinecraftClientLib minecraft,
                  int x,
                  int y,
                  int width,
                  int height,
                  @NotNull MinecraftTextComponent text,
                  @NotNull OnPress pressAction,
                  @NotNull OnTooltip tooltipAction) {
        super(minecraft, x, y, width, height, text);

        this.pressAction = pressAction;
        this.tooltipAction = tooltipAction;
    }

    // AbstractButton impl
    @Override
    public void onPress() {
        pressAction.onPress(this);
    }

    // GuiAbstractWidget impl
    @Override
    public void renderButton(@NotNull GuiRender render, int mouseX, int mouseY, float delta) {
        super.renderButton(render, mouseX, mouseY, delta);
        if (isHoveredOrFocused()) {
            renderToolTip(render, mouseX, mouseY);
        }
    }

    @Override
    public void renderToolTip(@NotNull GuiRender render, int mouseX, int mouseY) {
        tooltipAction.onTooltip(this, render, mouseX, mouseY);
    }

    @Override
    public void updateNarration(@NotNull NarrationOutput narrationOutput) {
        super.updateNarration(narrationOutput);
        tooltipAction.narrateTooltip(
                (component) -> narrationOutput.add(NarrationOutput.Type.HINT, component)
        );
    }

    public interface OnTooltip {

        void onTooltip(Button button, GuiRender render, int mouseX, int mouseY);

        default void narrateTooltip(Consumer<MinecraftTextComponent> consumer) {
        }
    }

    public interface OnPress {

        void onPress(Button button);
    }
}
