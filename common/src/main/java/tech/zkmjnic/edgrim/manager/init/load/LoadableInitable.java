package tech.zkmjnic.edgrim.manager.init.load;

import tech.zkmjnic.edgrim.manager.init.Initable;

public interface LoadableInitable extends Initable {
    void load();
}
