package tech.zkmjnic.edgrim.utils.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeleportAcceptData {
    boolean isTeleport;
    SetBackData setback;
    TeleportData teleportData;
}
