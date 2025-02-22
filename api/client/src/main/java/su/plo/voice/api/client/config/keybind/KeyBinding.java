package su.plo.voice.api.client.config.keybind;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface KeyBinding {

    @NotNull String getName();

    Set<Key> getKeys();

    boolean isAnyContext();

    boolean isPressed();

    void resetState();

    void updateState(@NotNull Action action);

    void addPressListener(@NotNull OnPress onPress);

    void removePressListener(@NotNull OnPress onPress);

    void clearPressListener();

    @FunctionalInterface
    interface OnPress {

        void onPress(@NotNull Action action);
    }

    enum Action {

        UP,
        DOWN,
        UNKNOWN;

        public static Action fromInt(int action) {
            return action == 0
                    ? KeyBinding.Action.UP
                    : action == 1
                    ? KeyBinding.Action.DOWN
                    : KeyBinding.Action.UNKNOWN;
        }
    }

    enum Type {

        KEYSYM,
        SCANCODE,
        MOUSE;

        private final Map<Integer, Key> keys = Maps.newHashMap();

        public Key getOrCreate(int keyCode) {
            return keys.computeIfAbsent(keyCode, (k) -> new Key(this, keyCode));
        }
    }

    @AllArgsConstructor
    final class Key {

        @Getter
        private final @NotNull Type type;
        @Getter
        private final int code;

        @Override
        public boolean equals(@Nullable Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                Key key = (Key) object;
                return this.code == key.code && this.type == key.type;
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(this.type, this.code);
        }
    }
}
