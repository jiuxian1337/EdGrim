package tech.zkmjnic.edgrim.checks.impl.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.checks.type.PostPredictionCheck;
import tech.zkmjnic.edgrim.player.EdGrimPlayer;

/**
 * @Author: siuxian_baka
 * @Date: 2026/3/12 09:57
 */
@CheckData(name = "NoSlowC (Blink)", configName = "NoSlowC", setback = 0)
public class NoSlowC extends Check implements PostPredictionCheck {

    public NoSlowC(EdGrimPlayer player) {
        super(player);
    }
    private boolean c08 = false;
    private boolean flag = false;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
            WrapperPlayClientPlayerFlying c03 = new WrapperPlayClientPlayerFlying(event);
            if (c03.hasPositionChanged()) {
                if (flag && player.isMoving()) flagAndAlertWithSetback("SlowedByUsingItem: " + player.packetStateData.isSlowedByUsingItem());
            }
            flag = false;
            c08 = false;
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging c07 = new WrapperPlayClientPlayerDigging(event);
            if (c07.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                if (c08) flag = true;
            }
        }
    }

    public void onC08() {
        if (player.isMoving()) c08 = true;
    }
}
