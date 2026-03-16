package tech.zkmjnic.edgrim.manager.init.stop;

import tech.zkmjnic.edgrim.manager.init.Initable;

public interface StoppableInitable extends Initable {
    void stop();
}
