package cc.watchneko.checks.impl.misc;

import cc.watchneko.checks.Check;
import cc.watchneko.checks.CheckData;
import cc.watchneko.player.PlayerData;

@CheckData(name = "TransactionOrder")
public class TransactionOrder extends Check {
    public TransactionOrder(PlayerData player) {
        super(player);
    }
}
