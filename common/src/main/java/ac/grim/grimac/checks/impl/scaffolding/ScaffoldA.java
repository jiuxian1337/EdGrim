package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

import java.util.ArrayList;
import java.util.List;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "ScaffoldA",
        configName = "ScaffoldA",
        decay = 0.86,
        description = "Scaffold analysis")
public final class ScaffoldA extends BlockPlaceCheck {
    private final List<Long> sides = new ArrayList<>();
    private final List<Long> surfaces = new ArrayList<>();
    private final List<Long> failed = new ArrayList<>();
    private final List<Integer> sneakTimer = new ArrayList<>();
    private final List<Integer> placeTimer = new ArrayList<>();
    private final List<Vector3dm> hitLocations = new ArrayList<>();
    private final int dragClickBuffer = 0;
    private long checkScaffold;
    private long click;
    private long place;
    private long lastAnalysisTime;
    private boolean checkingScaffold = false;
    private double buffer1 = 0;
    private double bufferFast = 0;
    private double checkBuffer1 = 0;
    private double checkBuffer2 = 0;
    private double checkBuffer3 = 0;
    private double PREVdIFF = 0;
    private double placeSpeed = 0;
    private int placeCounter = 0;
    private int tickCounter = 0;
    private int lastJump = 0;
    private int lastSneak = 0;
    private int sneakTiming = 0;
    private double godbridgeInARow = 0;
    private double diffTooLowInARow = 0;
    private int sneakTick = 0;
    private int placeTick = 0;
    private double dragClick = 0;
    private double godBridge = 0;
    private double buffer = 0;
    private double buffer2 = 0;
    private double prevScore = 0;
    private int lastPlaceY = -1;
    private double tooShortSneak = 0;
    private boolean ua = false;

    private float deltaYaw;
    private float deltaPitch;
    private float yaw;
    private float pitch;

    private boolean debug;
    private boolean checkGB;
    private boolean cancelGB;
    private boolean cancelPlace;

    public ScaffoldA(GrimPlayer player) {
        super(player);
        long now = time();
        checkScaffold = now;
        click = now;
        place = now;
        lastAnalysisTime = now;
    }

    private static long time() {
        return System.currentTimeMillis();
    }

    private static boolean hasTimeElapsed(long lastTime, long elapsedMs) {
        return time() - lastTime > elapsedMs;
    }

    @Override
    public void process(RotationUpdate rotationUpdate) {
        this.deltaYaw = rotationUpdate.getDeltaXRotABS();
        this.deltaPitch = rotationUpdate.getDeltaYRotABS();
        this.yaw = rotationUpdate.getTo().getYaw();
        this.pitch = rotationUpdate.getTo().getPitch();
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (place.isBlock) {

            Vector3dm blockPos;
            if (place.hitData != null) {
                blockPos = new Vector3dm(player.x, player.y, player.z).subtract(place.hitData.blockHitLocation());

                boolean yaw = deltaYaw >= 0 && deltaYaw < 4;
                boolean pitch = deltaPitch > 0.2 && deltaPitch < 4;
                boolean jittered = yaw && pitch;

                float pYaw = GrimMath.wrapAngleTo180(this.yaw);
                float offsetToNear45Deg = Math.abs(pYaw - ((float) (int) pYaw / 45) * 45f);
                if (offsetToNear45Deg < 1) {
                }

                hitLocations.add(blockPos);
                if (hitLocations.size() > 3) {
                    double XMean = 0;
                    double YMean = 0;
                    double ZMean = 0;
                    for (Vector3dm loc : hitLocations) {
                        XMean += loc.getX();
                        YMean += loc.getY();
                        ZMean += loc.getZ();
                    }
                    XMean /= hitLocations.size();
                    YMean /= hitLocations.size();
                    ZMean /= hitLocations.size();

                    double XStd = 0;
                    double YStd = 0;
                    double ZStd = 0;
                    for (Vector3dm loc : hitLocations) {
                        XStd += GrimMath.square(loc.getBlockX() - XMean);
                        YStd += GrimMath.square(loc.getBlockY() - YMean);
                        ZStd += GrimMath.square(loc.getBlockZ() - ZMean);
                    }
                    XStd /= hitLocations.size();
                    YStd /= hitLocations.size();
                    ZStd /= hitLocations.size();
                    double combinedStd = GrimMath.square(
                            XStd * XStd +
                                    YStd * YStd +
                                    ZStd * ZStd
                    );
                    double diff = Math.abs(prevScore - combinedStd);
                    double diff2 = Math.abs(PREVdIFF - diff);

                    if (!hasTimeElapsed(lastAnalysisTime, 6000)) {
                        if ((diff < 0.001 || diff2 < 0.01) && diff > 0.00001 && combinedStd > 0.00001) {
                            diffTooLowInARow += 1;
                        }
                    }

                    prevScore = combinedStd;
                    PREVdIFF = diff;
                    lastAnalysisTime = time();
                    hitLocations.clear();
                }

                if (place.itemStack.getType().getPlacedType() != null) {

                    placeCounter++;
                    checkScaffold = time();


                    switch (place.getFace()) {
                        case WEST, EAST, SOUTH, NORTH -> {
                            placeTimer.add(placeTick);
                            placeTick = 0;
                            sides.add(time());
                        }
                        case UP, DOWN -> {
                            placeTimer.add(placeTick);
                            placeTick = 0;
                            surfaces.add(time());
                        }
                        case OTHER -> failed.add(time());
                    }

                    if (placeTimer.size() > 15) {
                        placeTimer.remove(0);
                        double score = GrimMath.getStandardDeviation(placeTimer) * GrimMath.getAverage(placeTimer);
                        if (score < 1.2 && score > 0.7) {
                        } else {
                            buffer2 -= 0.4;
                            if (buffer2 < 0) buffer2 = 0;
                        }
                    }

                    int c = surfaces.size() + failed.size();
                    double possibility = sides.isEmpty() ? 0 : Math.min((1 - ((double) c / sides.size())), 1);

                    if (buffer1 > 3 && !sides.isEmpty() && possibility > 0.5) {
                        if (checkBuffer1++ > 3) {
                            if (flagAndAlert("(Auto#1)p= " + String.format("%.2f%%", possibility * 100)) && shouldCancel()) {
                                cancelPlace = true;
                            }
                        }
                    } else {
                        if (debug) alert("Auto#1 bypass\n" +
                                place.getFace().toString() + "\n" +
                                "buffer1= " + buffer1 + "\n" +
                                "sides= " + sides.size() + "\n" +
                                "possibility= " + possibility);
                        checkBuffer1 = Math.max(0, buffer1 - 0.1);
                    }

                    if (!sides.isEmpty() && possibility > 0.5 && lastJump > 5 && this.pitch >= 45.0F) {
                        if (checkBuffer2++ > 7) {
                            if (flagAndAlert("(Auto#2)\np= " + String.format("%.2f%%", possibility * 100)) && shouldCancel()) {
                                cancelPlace = true;
                            }
                        }
                    }

                    if (sides.size() > 3 && lastSneak > 5 && surfaces.isEmpty() && this.pitch >= 45.0F) {
                        if (checkBuffer3++ > 5) {
                            if (flagAndAlert("(Sneak)\nls= " + lastSneak + "\nsd= " + sides.size()) && shouldCancel()) {
                                cancelPlace = true;
                            }
                        }
                    }

                    if (sides.size() > 4 && lastSneak > 2 && surfaces.isEmpty() && tooShortSneak > 2.6 && this.pitch >= 45.0F) {
                        if (checkBuffer3++ > 5) {
                            if (flagAndAlert("(Sneak#2)\nls= " + lastSneak + "\nsd= " + sides.size() + "\ntss= " + (int) tooShortSneak) && shouldCancel()) {
                                cancelPlace = true;
                            }
                        }
                    }

                    if (place.getFace() != BlockFace.OTHER
                            && place.getFace() != BlockFace.UP
                            && place.getFace() != BlockFace.DOWN
                    ) {
                        if (place.position.getY() == lastPlaceY) {
                            if (lastSneak > 5 && this.pitch >= 45.0F && dragClick < 2) {
                                godBridge = Math.min(5, godBridge + 1);
                                if (debug) alert("godBridge++\n" +
                                        place.getFace().toString() + "\n" +
                                        "dc= " + dragClick + "\n" +
                                        "ls= " + lastSneak + "\n" +
                                        "pitch= " + this.pitch);
                            } else if (lastSneak <= 5 || dragClick > 1) godBridge = 0;
                        } else {
                            godBridge = Math.max(0, godBridge - 1.5);
                        }
                        lastPlaceY = place.position.getY();
                    }

                    if (dragClick < 5 && godBridge > 3 && checkGB) {

                        if (time() - click > 1000) {
                            if (debug) alert("godBridge = 0\n" +
                                    place.getFace().toString() + "\n" +
                                    "dc= " + dragClick + "\n" +
                                    "lc= " + (time() - click));
                            godBridge = 0;
                        } else if (godbridgeInARow++ > 3 && flagAndAlert("(GodBridge/KeepY)\ndc= " + dragClick + "\nlc= " + (time() - click)) && shouldCancel() && cancelGB) {
                            cancelPlace = true;
                        }
                    } else {
                        godbridgeInARow = Math.max(godbridgeInARow - (placeSpeed <= 0.6 ? 1 : 0.6), 0);
                    }


                    if (cancelPlace && shouldModifyPackets()) {
                        place.resync();
                        cancelPlace = false;
                    }
                }

            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isFlying(event.getPacketType())) {
            diffTooLowInARow = Math.max(diffTooLowInARow - 0.2 / 20.0, 0);
            tickCounter++;
            if (tickCounter > 30) {
                placeSpeed = (double) placeCounter / tickCounter;
                place = time();
                tickCounter = 0;
                placeCounter = 0;
            }
            lastJump++;
            lastSneak++;
            sneakTick++;
            placeTick++;
            final SimpleCollisionBox box = player.boundingBox.copy();
            final int blockX = GrimMath.floor((box.maxX + box.minX) / 2);
            final int blockY = GrimMath.floor(box.minY - 0.5);
            final int blockZ = GrimMath.floor((box.maxZ + box.minZ) / 2);
            boolean underAir = player.compensatedWorld.getBlock(
                    new Vector3dm(
                            blockX,
                            blockY,
                            blockZ
                    )
            ).getType().equals(StateTypes.AIR);
            if (player.isSneaking) {
                lastSneak = 0;
                if (sneakTiming == 0) {
                    ua = underAir;
                }
                sneakTiming++;
            } else {
                if (sneakTiming < 5 && !underAir && ua) {
                    tooShortSneak++;
                    if (tooShortSneak > 3) tooShortSneak = 0;
                }
                sneakTiming = 0;
            }
            if (Math.abs((player.y - player.lastY) - 0.42) < 0.05) {
                lastJump = 0;
            }

            checkingScaffold = !hasTimeElapsed(checkScaffold, 2000);

            if (!checkingScaffold) {
                sides.clear();
                surfaces.clear();
            }

            tooShortSneak = Math.max(0, tooShortSneak - 0.13);

            checkBuffer1 = Math.max(0, buffer1 - 0.08);
            checkBuffer2 = Math.max(0, checkBuffer2 - 0.17);
            checkBuffer3 = Math.max(0, checkBuffer2 - 0.17);

            buffer1 = Math.max(0, buffer1 - 0.2);
            if (deltaYaw > 20 && (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION)) {
                buffer1 = Math.min(buffer1 + 1, 5);
                if (debug) alert("buffer1 + 1\n" +
                        "buffer1= " + buffer1 + "\n" +
                        "deltaYaw= " + deltaYaw);
                if (deltaYaw > 125) {
                    bufferFast = Math.min(++bufferFast, 25);
                    if (bufferFast >= 25 && flagAndAlert("(FastRot)\n" +
                            "buffer= " + bufferFast + "\n" +
                            "deltaYaw= " + deltaYaw) && shouldCancel()) {
                        cancelPlace = true;
                    }
                } else {
                    bufferFast = Math.max(bufferFast - 0.25, 0);

                }
            }

            long maxTime = 2000;
            long now = time();
            sides.removeIf(t -> now - t > maxTime);
            surfaces.removeIf(t -> now - t > maxTime);
            failed.removeIf(t -> now - t > maxTime);

            if (sides.size() > 300 || surfaces.size() > 300 || failed.size() > 300) {
                if (flagAndAlert("(Fast)\ns= " + sides.size() + "\nsf= " + surfaces.size() + "\nf= " + failed.size() + "\nf= " + failed.size())) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }

            while (sides.size() > 300) {
                sides.remove(0);
            }
            while (surfaces.size() > 300) {
                surfaces.remove(0);
            }
            while (failed.size() > 300) {
                surfaces.remove(0);
            }
        } else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction action = new WrapperPlayClientEntityAction(event);

            if (action.getAction() == WrapperPlayClientEntityAction.Action.START_SNEAKING) {
                sneakTick = 0;
            } else if (action.getAction() == WrapperPlayClientEntityAction.Action.STOP_SNEAKING) {
                sneakTimer.add(sneakTick);
                while (sneakTimer.size() > 10) {
                    sneakTimer.remove(0);
                }
                if (sneakTimer.size() == 10 && checkingScaffold) {
                    double stdDev = GrimMath.getStandardDeviation(sneakTimer);
                    if (stdDev < 0.2) {
                        if (buffer++ > 3) {
                            flagAndAlert("(Change)\nstd= " + stdDev);
                        }
                    } else {
                        buffer -= 0.2;
                        if (buffer1 < 0) {
                            buffer = 0;
                        }
                    }
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {

            if (hasTimeElapsed(click, 50)) {
                dragClick = Math.max(0.0, dragClick - 1);
            } else {
                dragClick = Math.min(20,  dragClick + 1);
                if (debug) alert("dragClick++\n" +
                        (time() - click));
            }
            click = time();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        debug = config.getBooleanElse("ScaffoldA.debug", false);
        checkGB = config.getBooleanElse("ScaffoldA.check-low-cps-god-bridge.enable", true);
        cancelGB = config.getBooleanElse("ScaffoldA.check-low-cps-god-bridge.cancel", true);
    }
}
