package su.plo.voice.client.audio.device;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.device.*;
import su.plo.voice.api.client.audio.device.source.AlSource;
import su.plo.voice.api.client.audio.device.source.SourceGroup;
import su.plo.voice.api.client.connection.ServerInfo;
import su.plo.voice.api.util.Params;
import su.plo.voice.client.audio.device.source.VoiceOutputSourceGroup;
import su.plo.voice.client.audio.filter.*;
import su.plo.voice.client.config.ClientConfig;

import javax.sound.sampled.AudioFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

@RequiredArgsConstructor
public final class VoiceDeviceManager implements DeviceManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final PlasmoVoiceClient voiceClient;
    private final ClientConfig config;

    private final List<AudioDevice> inputDevices = new CopyOnWriteArrayList<>();
    private final List<AudioDevice> outputDevices = new CopyOnWriteArrayList<>();

    @Override
    public void add(@NotNull AudioDevice device) {
        checkNotNull(device, "device cannot be null");

        List<AudioDevice> devices = getDevicesList(device);

        if (devices == inputDevices && devices.size() > 0) {
            throw new IllegalStateException("Multiple input devices currently are not supported. Use DeviceManager::replace to replace the current input device");
        }

        if (devices.contains(device)) return;

        voiceClient.getEventBus().register(voiceClient, device);
        devices.add(device);
    }

    @Override
    public void replace(@Nullable AudioDevice oldDevice, @NotNull AudioDevice newDevice) {
        checkNotNull(newDevice, "newDevice cannot be null");

        List<AudioDevice> devices = getDevicesList(newDevice);

        if (oldDevice != null) {
            if (devices != getDevicesList(oldDevice)) {
                throw new IllegalArgumentException("Devices are not implementing the same interface");
            }

            int index = devices.indexOf(oldDevice);
            if (index < 0) {
                throw new IllegalArgumentException("oldDevice not found in device list");
            }

            devices.set(index, newDevice);
        } else {
            if (devices.size() > 0) {
                oldDevice = devices.get(0);

                devices.set(0, newDevice);

                oldDevice.close();
            } else devices.add(newDevice);
        }

        if (oldDevice != null) voiceClient.getEventBus().unregister(voiceClient, oldDevice);
        voiceClient.getEventBus().register(voiceClient, newDevice);
    }

    @Override
    public void remove(@NotNull AudioDevice device) {
        checkNotNull(device, "device cannot be null");
        getDevicesList(device).remove(device);
        voiceClient.getEventBus().unregister(voiceClient, device);
    }

    @Override
    public void clear(@Nullable DeviceType type) {
        if (type == DeviceType.INPUT) {
            inputDevices.forEach(device -> {
                device.close();
                voiceClient.getEventBus().unregister(voiceClient, device);
            });
            inputDevices.clear();
        } else if (type == DeviceType.OUTPUT) {
            outputDevices.forEach(device -> {
                device.close();
                voiceClient.getEventBus().unregister(voiceClient, device);
            });
            outputDevices.clear();
        } else {
            getDevices(null).forEach(device -> {
                device.close();
                voiceClient.getEventBus().unregister(voiceClient, device);
            });
            inputDevices.clear();
            outputDevices.clear();
        }
    }

    @Override
    public <T extends AudioDevice> Collection<T> getDevices(DeviceType type) {
        if (type == DeviceType.INPUT) {
            return (Collection<T>) inputDevices;
        } else if (type == DeviceType.OUTPUT) {
            return (Collection<T>) outputDevices;
        } else {
            ImmutableList.Builder<AudioDevice> builder = ImmutableList.builder();
            return (Collection<T>) builder.addAll(inputDevices).addAll(outputDevices).build();
        }
    }

    @Override
    public SourceGroup createSourceGroup(@Nullable DeviceType type) {
        if (type == DeviceType.OUTPUT) return new VoiceOutputSourceGroup(this);
        throw new IllegalArgumentException(type + " not supported");
    }

    @Override
    public InputDevice openInputDevice(@Nullable AudioFormat format, @NotNull Params params) throws Exception {
        if (format == null) {
            if (!voiceClient.getServerInfo().isPresent()) throw new IllegalStateException("Not connected");

            ServerInfo serverInfo = voiceClient.getServerInfo().get();
            format = serverInfo.getVoiceInfo().getFormat(
                    config.getVoice().getStereoCapture().value()
            );
        }

        InputDevice device;
        if (config.getVoice().getUseJavaxInput().value()) {
            device = openJavaxInputDevice(format);
        } else {
            try {
                device = openAlInputDevice(format);
            } catch (Exception e) {
                LOGGER.error("Failed to open OpenAL input device, falling back to Javax input device", e);

                device = openJavaxInputDevice(format);
            }
        }

        // apply default filters
        device.addFilter(new StereoToMonoFilter(config.getVoice().getStereoCapture()));
        device.addFilter(new GainFilter(config.getVoice().getMicrophoneVolume()));
        device.addFilter(new NoiseSuppressionFilter((int) format.getSampleRate(), config.getVoice().getNoiseSuppression()));

        return device;
    }

    @Override
    public OutputDevice<AlSource> openOutputDevice(@Nullable AudioFormat format, @NotNull Params params) throws Exception {
        Optional<DeviceFactory> deviceFactory = voiceClient.getDeviceFactoryManager().getDeviceFactory("AL_OUTPUT");
        if (!deviceFactory.isPresent()) {
            throw new DeviceException("OpenAL output device factory is not initialized");
        }

        if (format == null) {
            if (!voiceClient.getServerInfo().isPresent()) throw new IllegalStateException("Not connected");

            ServerInfo serverInfo = voiceClient.getServerInfo().get();
            format = serverInfo.getVoiceInfo().getFormat(false);
        }

        AudioDevice device = deviceFactory.get().openDevice(
                format,
                config.getVoice().getOutputDevice().value(),
                Params.builder()
                        .set("hrtf", config.getVoice().getHrtf().value())
                        .set("listenerCameraRelative", config.getVoice().getListenerCameraRelative().value())
                        .build()
        );

        device.addFilter(new CompressorFilter(
                (int) format.getSampleRate(),
                config.getVoice().getCompressorLimiter(),
                config.getAdvanced().getCompressorThreshold()
        ));
        device.addFilter(new LimiterFilter(
                (int) format.getSampleRate(),
                config.getVoice().getCompressorLimiter(),
                config.getAdvanced().getLimiterThreshold()
        ));

        return (OutputDevice<AlSource>) device;
    }

    private InputDevice openAlInputDevice(@NotNull AudioFormat format) throws Exception {
        Optional<DeviceFactory> deviceFactory = voiceClient.getDeviceFactoryManager().getDeviceFactory("AL_INPUT");
        if (!deviceFactory.isPresent()) throw new IllegalStateException("OpenAL input factory is not registered");

        return (InputDevice) deviceFactory.get().openDevice(
                format,
                Strings.emptyToNull(config.getVoice().getInputDevice().value()),
                Params.EMPTY
        );
    }

    private InputDevice openJavaxInputDevice(@NotNull AudioFormat format) throws Exception {
        Optional<DeviceFactory> deviceFactory = voiceClient.getDeviceFactoryManager().getDeviceFactory("JAVAX_INPUT");
        if (!deviceFactory.isPresent()) throw new IllegalStateException("Javax input factory is not registered");

        return (InputDevice) deviceFactory.get().openDevice(
                format,
                Strings.emptyToNull(config.getVoice().getInputDevice().value()),
                Params.EMPTY
        );
    }

    private List<AudioDevice> getDevicesList(AudioDevice device) {
        List<AudioDevice> devices;
        if (device instanceof InputDevice) {
            devices = inputDevices;
        } else if (device instanceof OutputDevice) {
            devices = outputDevices;
        } else {
            throw new IllegalArgumentException("device not implements InputDevice or OutputDevice");
        }

        return devices;
    }
}
