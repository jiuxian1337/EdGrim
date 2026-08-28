package cc.watchneko.manager;

import cc.watchneko.WatchNekoAPI;
import cc.watchneko.manager.init.Initable;
import cc.watchneko.manager.init.load.LoadableInitable;
import cc.watchneko.manager.init.load.PacketEventsInit;
import cc.watchneko.manager.init.start.*;
import cc.watchneko.manager.init.stop.StoppableInitable;
import cc.watchneko.manager.init.stop.TerminatePacketEvents;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.utils.anticheat.LogUtil;
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
            if (initable instanceof LoadableInitable)
                extraLoadableInitables.add((LoadableInitable) initable);
            if (initable instanceof StartableInitable)
                extraStartableInitables.add((StartableInitable) initable);
            if (initable instanceof StoppableInitable)
                extraStoppableInitables.add((StoppableInitable) initable);
        }

        initializersOnLoad = ImmutableList.<LoadableInitable>builder()
                .add(new PacketEventsInit(packetEventsAPI))
                .add(() -> WatchNekoAPI.INSTANCE.getExternalAPI().load())
                .addAll(extraLoadableInitables)
                .build();

        initializersOnStart = ImmutableList.<StartableInitable>builder()
                .add(WatchNekoAPI.INSTANCE.getExternalAPI())
                .add(new PacketManager())
                .add(new ViaBackwardsManager())
                .add(new TickRunner())
                .add(new CommandRegister(commandManager))
                .add(new PacketLimiter())
                .add(WatchNekoAPI.INSTANCE.getAlertManager())
                .add(WatchNekoAPI.INSTANCE.getDiscordManager())
                .add(WatchNekoAPI.INSTANCE.getSpectateManager())
                .add(WatchNekoAPI.INSTANCE.getViolationDatabaseManager())
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
