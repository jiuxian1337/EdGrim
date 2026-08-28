package cc.watchneko;

import ac.grim.grimac.api.event.EventBus;
import ac.grim.grimac.api.event.OptimizedEventBus;
import ac.grim.grimac.api.plugin.GrimPlugin;
import cc.watchneko.manager.*;
import cc.watchneko.manager.config.BaseConfigManager;
import cc.watchneko.manager.init.Initable;
import cc.watchneko.manager.violationdatabase.ViolationDatabaseManager;
import cc.watchneko.platform.api.Platform;
import cc.watchneko.platform.api.PlatformLoader;
import cc.watchneko.platform.api.PlatformServer;
import cc.watchneko.platform.api.manager.*;
import cc.watchneko.platform.api.player.PlatformPlayerFactory;
import cc.watchneko.platform.api.scheduler.PlatformScheduler;
import cc.watchneko.platform.api.sender.Sender;
import cc.watchneko.platform.api.sender.SenderFactory;
import cc.watchneko.utils.anticheat.PlayerDataManager;
import cc.watchneko.utils.common.GrimArguments;
import cc.watchneko.utils.reflection.ReflectionUtils;
import lombok.Getter;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;


@Getter
public final class WatchNekoAPI {
    public static final WatchNekoAPI INSTANCE = new WatchNekoAPI();

    @Getter
    private final Platform platform = detectPlatform();
    private final BaseConfigManager configManager;
    private final AlertManagerImpl alertManager;
    private final SpectateManager spectateManager;
    private final DiscordManager discordManager;
    private final PlayerDataManager playerDataManager;
    private final TickManager tickManager;
    private final EventBus eventBus;
    private final WatchNekoExternalAPI externalAPI;
    private ViolationDatabaseManager violationDatabaseManager;
    private PlatformLoader loader;
    @Getter
    private InitManager initManager;
    private boolean initialized = false;

    private WatchNekoAPI() {
        this.configManager = new BaseConfigManager();
        this.alertManager = new AlertManagerImpl();
        this.spectateManager = new SpectateManager();
        this.discordManager = new DiscordManager();
        this.playerDataManager = new PlayerDataManager();
        this.tickManager = new TickManager();
        this.eventBus = new OptimizedEventBus();
        this.externalAPI = new WatchNekoExternalAPI(this);
    }

    // the order matters
    private static Platform detectPlatform() {
        Platform override = Platform.getByName(GrimArguments.PLATFORM_OVERRIDE);
        if (override != null) return override;
        if (ReflectionUtils.hasClass("io.papermc.paper.threadedregions.RegionizedServer"))
            return Platform.FOLIA;
        if (ReflectionUtils.hasClass("org.bukkit.Bukkit")) return Platform.BUKKIT;
        throw new IllegalStateException("Unknown platform!");
    }

    public void load(PlatformLoader platformLoader, Initable... platformSpecificInitables) {
        this.loader = platformLoader;
        this.violationDatabaseManager = new ViolationDatabaseManager(getGrimPlugin());
        this.initManager = new InitManager(loader.getPacketEvents(), loader::getCommandManager, platformSpecificInitables);
        this.initManager.load();
        this.initialized = true;
    }

    public void start() {
        checkInitialized();
        initManager.start();
    }

    public void stop() {
        checkInitialized();
        initManager.stop();
    }

    public PlatformScheduler getScheduler() {
        return loader.getScheduler();
    }

    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return loader.getPlatformPlayerFactory();
    }

    public CommandAdapter getCommandAdapter() {
        return loader.getCommandAdapter();
    }

    public GrimPlugin getGrimPlugin() {
        return loader.getPlugin();
    }

    public SenderFactory<?> getSenderFactory() {
        return loader.getSenderFactory();
    }

    public ItemResetHandler getItemResetHandler() {
        return loader.getItemResetHandler();
    }

    public PlatformPluginManager getPluginManager() {
        return loader.getPluginManager();
    }

    public PlatformServer getPlatformServer() {
        return loader.getPlatformServer();
    }

    public @NotNull MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return loader.getMessagePlaceHolderManager();
    }

    public CommandManager<Sender> getCommandManager() {
        return loader.getCommandManager();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("WatchNekoAPI has not been initialized!");
        }
    }

    public PermissionRegistrationManager getPermissionManager() {
        return loader.getPermissionManager();
    }
}
