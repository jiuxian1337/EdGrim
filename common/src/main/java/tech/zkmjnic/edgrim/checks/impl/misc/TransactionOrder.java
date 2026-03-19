package tech.zkmjnic.edgrim.checks.impl.misc;

import tech.zkmjnic.edgrim.checks.Check;
import tech.zkmjnic.edgrim.checks.CheckData;
import tech.zkmjnic.edgrim.player.PlayerData;

@CheckData(name = "TransactionOrder")
public class TransactionOrder extends Check {
    public TransactionOrder(PlayerData player) {
        super(player);
    }
}
