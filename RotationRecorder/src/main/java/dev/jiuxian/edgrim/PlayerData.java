package dev.jiuxian.edgrim;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.UUID;

public class PlayerData {
    public final UUID uuid;
    public final String name;
    
    public final ArrayDeque<RotationFrame> tickBuffer = new ArrayDeque<>();
    public final ArrayDeque<Long> attacksAwaitingCenter = new ArrayDeque<>();
    public final ArrayDeque<PendingSample> pendingSamples = new ArrayDeque<>();
    
    public long rotationSequence = 0;
    
    public float lastYaw = 0;
    public float lastPitch = 0;
    public boolean isFirstRotation = true;
    
    public final File sessionFile;
    
    public PlayerData(UUID uuid, String name, File playerDir) {
        this.uuid = uuid;
        this.name = name;
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        this.sessionFile = new File(playerDir, name + "-" + timestamp + ".json");
    }
}
