package io.semla.relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import static javax.persistence.FetchType.EAGER;

public enum IncludeType {

    NONE(0),
    FETCH(1),
    CREATE(2),
    UPDATE(4),
    DELETE(8),
    ALL(2 + 4 + 8),
    DELETE_ORPHANS(16);

    private static final Logger LOGGER = LoggerFactory.getLogger(IncludeType.class);
    private final int value;

    IncludeType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean isContainedIn(int value) {
        return (value & this.value) == this.value;
    }

    public static IncludeType fromCascadeType(CascadeType cascadeType) {
        switch (cascadeType) {
            case ALL:
                return ALL;
            case PERSIST:
                return CREATE;
            case MERGE:
                return UPDATE;
            case REMOVE:
                return DELETE;
            default:
                LOGGER.warn("cascadeType " + cascadeType + " is not handled, ignoring...");
                return NONE;
        }
    }

    public static IncludeType fromFetchType(FetchType fetch) {
        return fetch == EAGER ? FETCH : NONE;
    }

    public static IncludeType fromOrphanRemoval(boolean orphanRemoval) {
        return orphanRemoval ? DELETE_ORPHANS : NONE;
    }
}
