package tech.zkmjnic.edgrim.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PacketCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;
import tech.zkmjnic.edgrim.utils.math.MathUtil;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported behaviour from MX-Project AutoClickerCheck.
 *
 * This check looks for unnatural stability in swing delays (in ticks),
 * using per-window kurtosis and entropy jitter ("jiff delta") analysis.
 */
@CheckData(
        name = "AutoclickerA",
        description = "Detects stable, macro-like clicking patterns during combat"
)
public final class AutoclickerA extends Check implements PacketCheck {

    // MX-style defaults
    private static final int DEFAULT_MAX_DELAY_TICKS = 25;
    private static final int DEFAULT_STACK_SIZE = 100;
    private static final int DEFAULT_WINDOW_SIZE = 20;
    private static final long DEFAULT_REQUIRE_MOVE_WITHIN_MS = 500L;
    private static final long DEFAULT_REQUIRE_ATTACK_WITHIN_MS = 7000L;
    private static final double DEFAULT_ENTROPY_JIFF_MIN = 0.04;
    private static final double DEFAULT_ENTROPY_JIFF_MAX = 0.06;

    // Config
    private boolean enabled = true;
    private int maxDelayTicks = DEFAULT_MAX_DELAY_TICKS;
    private int stackSize = DEFAULT_STACK_SIZE;
    private int windowSize = DEFAULT_WINDOW_SIZE;
    private long requireMoveWithinMs = DEFAULT_REQUIRE_MOVE_WITHIN_MS;
    private long requireAttackWithinMs = DEFAULT_REQUIRE_ATTACK_WITHIN_MS;
    private double entropyJiffMin = DEFAULT_ENTROPY_JIFF_MIN;
    private double entropyJiffMax = DEFAULT_ENTROPY_JIFF_MAX;

    // State
    private long oldTime = System.currentTimeMillis();
    private long lastMove = System.currentTimeMillis();
    private long lastAttack = System.currentTimeMillis();
    private boolean collectClicks;
    private final List<Long> stack = new ArrayList<>(DEFAULT_STACK_SIZE + 10);
    private boolean entropyQuery;

    public AutoclickerA(final EdGrimPlayer player) {
        super(player);
    }

    @Override
    public void onReload(final ConfigManager config) {
        enabled = config.getBooleanElse("AutoclickerA.enabled", true);
        maxDelayTicks = clamp(config.getIntElse("AutoclickerA.max-delay-ticks", DEFAULT_MAX_DELAY_TICKS), 3, 60);
        stackSize = clamp(config.getIntElse("AutoclickerA.stack-size", DEFAULT_STACK_SIZE), 20, 300);
        windowSize = clamp(config.getIntElse("AutoclickerA.window-size", DEFAULT_WINDOW_SIZE), 5, 60);
        requireMoveWithinMs = clamp(config.getLongElse("AutoclickerA.require-move-within-ms", DEFAULT_REQUIRE_MOVE_WITHIN_MS), 0L, 5000L);
        requireAttackWithinMs = clamp(config.getLongElse("AutoclickerA.require-attack-within-ms", DEFAULT_REQUIRE_ATTACK_WITHIN_MS), 250L, 15000L);
        entropyJiffMin = config.getDoubleElse("AutoclickerA.entropy-jiff-min", DEFAULT_ENTROPY_JIFF_MIN);
        entropyJiffMax = config.getDoubleElse("AutoclickerA.entropy-jiff-max", DEFAULT_ENTROPY_JIFF_MAX);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!isEnabled() || !enabled) {
            return;
        }

        // Treat tick packets as "activity" for lastMove gating.
        if (isTickPacketIncludingNonMovement(event.getPacketType()) || event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
            lastMove = System.currentTimeMillis();
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            final WrapperPlayClientPlayerDigging digging = new WrapperPlayClientPlayerDigging(event);
            final DiggingAction action = digging.getAction();
            if (action == DiggingAction.START_DIGGING || action == DiggingAction.FINISHED_DIGGING || action == DiggingAction.CANCELLED_DIGGING) {
                collectClicks = false;
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            final WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                lastAttack = System.currentTimeMillis();
                collectClicks = true;
            }
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) {
            return;
        }

        final long now = System.currentTimeMillis();
        final long delayTicks = (now - oldTime) / 50L;
        oldTime = now;

        if (delayTicks <= 0L || delayTicks >= maxDelayTicks) {
            return;
        }

        if (!collectClicks) {
            return;
        }

        if (requireMoveWithinMs > 0 && lastMove + requireMoveWithinMs <= now) {
            return;
        }

        if (requireAttackWithinMs > 0 && lastAttack + requireAttackWithinMs <= now) {
            return;
        }

        stack.add(delayTicks);
        if (stack.size() > stackSize) {
            analyze();
        }
    }

    private void analyze() {
        final List<Double> kurtosisStack = new ArrayList<>();
        final List<Double> shannonStack = new ArrayList<>();
        final List<Double> localDelta = new ArrayList<>(windowSize);

        for (long delay : stack) {
            localDelta.add((double) delay);
            if (localDelta.size() >= windowSize) {
                kurtosisStack.add(MathUtil.getKurtosis(localDelta, false));
                shannonStack.add(shannonEntropy(localDelta));
                localDelta.clear();
            }
        }

        if (kurtosisStack.isEmpty()) {
            stack.clear();
            return;
        }

        final double maxKurtosis = MathUtil.getMax(kurtosisStack);
        if (maxKurtosis < 0.0) {
            flagAndAlert(String.format("t=mx-kurtosis max=%.3f n=%d", maxKurtosis, stack.size()));
            stack.clear();
            entropyQuery = false;
            return;
        }

        final List<Float> jiff = MathUtil.getJiffDelta(shannonStack, 2);
        if (!jiff.isEmpty()) {
            final double min = MathUtil.getMin(jiff);
            final double max = MathUtil.getMax(jiff);

            if (min < entropyJiffMin && max < entropyJiffMax) {
                if (!entropyQuery) {
                    entropyQuery = true;
                } else {
                    flagAndAlert(String.format("t=mx-entropy min=%.3f max=%.3f n=%d", min, max, stack.size()));
                    entropyQuery = false;
                }
            } else {
                entropyQuery = false;
            }
        } else {
            entropyQuery = false;
        }

        stack.clear();
    }

    private static double shannonEntropy(final List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        final Map<Integer, Integer> counts = new HashMap<>();
        for (double v : values) {
            int k = (int) v;
            counts.put(k, counts.getOrDefault(k, 0) + 1);
        }

        final int n = values.size();
        double entropy = 0.0;
        for (int c : counts.values()) {
            final double p = c / (double) n;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static long clamp(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}
