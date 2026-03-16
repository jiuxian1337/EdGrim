package tech.zkmjnic.edgrim.manager;

import tech.zkmjnic.edgrim.EdGrimAPI;
import tech.zkmjnic.edgrim.manager.init.Initable;
import tech.zkmjnic.edgrim.manager.init.load.LoadableInitable;
import tech.zkmjnic.edgrim.manager.init.load.PacketEventsInit;
import tech.zkmjnic.edgrim.manager.init.start.*;
import tech.zkmjnic.edgrim.manager.init.start.*;
import tech.zkmjnic.edgrim.manager.init.start.*;
import tech.zkmjnic.edgrim.manager.init.stop.StoppableInitable;
import tech.zkmjnic.edgrim.manager.init.stop.TerminatePacketEvents;
import tech.zkmjnic.edgrim.platform.api.sender.Sender;
import tech.zkmjnic.edgrim.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.incendo.cloud.CommandManager;

import java.util.ArrayList;
import java.util.function.Supplier;

public class InitManager {

    private final ImmutableList<LoadableInitable> initializersOnLoad;
    private final ImmutableList<StartableInitable> initializersOnStart;
    private final ImmutableList<StoppableInitable> initializersOnStop;

    @Getter
    private boolean loaded = false;
    @Getter
    private boolean started = false;
    @Getter
    private boolean stopped = false;

    public InitManager(PacketEventsAPI<?> packetEventsAPI, Supplier<CommandManager<Sender>> commandManager, Initable... platformSpecificInitables) {
        ArrayList<LoadableInitable> extraLoadableInitables = new ArrayList<>();
        ArrayList<StartableInitable> extraStartableInitables = new ArrayList<>();
        ArrayList<StoppableInitable> extraStoppableInitables = new ArrayList<>();
        for (Initable initable : platformSpecificInitables) {
            if (initable instanceof LoadableInitable) extraLoadableInitables.add((LoadableInitable) initable);
            if (initable instanceof StartableInitable) extraStartableInitables.add((StartableInitable) initable);
            if (initable instanceof StoppableInitable) extraStoppableInitables.add((StoppableInitable) initable);
        }

        initializersOnLoad = ImmutableList.<LoadableInitable>builder()
                .add(new PacketEventsInit(packetEventsAPI))
                .add(() -> EdGrimAPI.INSTANCE.getExternalAPI().load())
                .addAll(extraLoadableInitables)
                .build();

        initializersOnStart = ImmutableList.<StartableInitable>builder()
                .add(EdGrimAPI.INSTANCE.getExternalAPI())
                .add(new PacketManager())
                .add(new ViaBackwardsManager())
                .add(new TickRunner())
                .add(new CommandRegister(commandManager))
                .add(new PacketLimiter())
                .add(EdGrimAPI.INSTANCE.getAlertManager())
                .add(EdGrimAPI.INSTANCE.getDiscordManager())
                .add(EdGrimAPI.INSTANCE.getSpectateManager())
                .add(EdGrimAPI.INSTANCE.getViolationDatabaseManager())
                .add(new JavaVersion())
                .add(new ViaVersion())
                .add(new TAB())
                .addAll(extraStartableInitables)
                .build();

        initializersOnStop = ImmutableList.<StoppableInitable>builder()
                .add(new TerminatePacketEvents())
                .addAll(extraStoppableInitables)
                .build();
    }

    public void load() {
        for (LoadableInitable initable : initializersOnLoad)
            try {
                initable.load();
            } catch (Exception e) {
                LogUtil.error("Failed to load " + initable.getClass().getSimpleName(), e);
            }
        loaded = true;
    }

    public void start() {
        for (StartableInitable initable : initializersOnStart)
            try {
                initable.start();
            } catch (Exception e) {
                LogUtil.error("Failed to start " + initable.getClass().getSimpleName(), e);
            }
        started = true;
    }

    public void stop() {
        for (StoppableInitable initable : initializersOnStop)
            try {
                initable.stop();
            } catch (Exception e) {
                LogUtil.error("Failed to stop " + initable.getClass().getSimpleName(), e);
            }
        stopped = true;
    }
}
