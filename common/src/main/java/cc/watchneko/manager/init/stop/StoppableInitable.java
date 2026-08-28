package cc.watchneko.manager.init.stop;

import cc.watchneko.manager.init.Initable;

public interface StoppableInitable extends Initable {
    void stop();
}
