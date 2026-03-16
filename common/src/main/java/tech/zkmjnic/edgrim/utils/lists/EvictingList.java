package tech.zkmjnic.edgrim.utils.lists;

import java.util.ArrayList;

public class EvictingList<K> extends ArrayList<K> {
    private final int maxSize;

    public EvictingList(int size) {
        this.maxSize = size;
    }

    @Override
    public boolean add(K k) {
        boolean r = super.add(k);
        if (size() > maxSize) {
            removeRange(0, size() - maxSize);
        }
        return r;
    }

    public boolean isFull() {
        return size() >= maxSize;
    }
}
