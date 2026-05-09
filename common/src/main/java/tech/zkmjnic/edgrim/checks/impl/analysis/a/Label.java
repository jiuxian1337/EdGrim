package tech.zkmjnic.edgrim.checks.impl.analysis.a;

public enum Label {
    LEGIT_LIKE,
    CHEAT_LIKE,
    UNCERTAIN;

    public String logName() {
        return switch (this) {
            case LEGIT_LIKE -> "legit-like";
            case CHEAT_LIKE -> "cheat-like";
            case UNCERTAIN -> "uncertain";
        };
    }
}
