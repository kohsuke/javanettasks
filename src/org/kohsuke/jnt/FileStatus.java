package org.kohsuke.jnt;

import java.util.Map;
import java.util.HashMap;

/**
 * @author Kohsuke Kawaguchi
 */
public enum FileStatus {

    DRAFT("Draft"),
    REVIEWED("Reviewed"),
    BASELINED("Baselined"),
    STABLE("Stable"),
    ARCHIVAL("Archival"),
    OBSOLETE("Obsolete");

    private static final Map index = new HashMap();

    private final String name;

    FileStatus(String name) {
        this.name = name;
    }

    /**
     * Returns the human-readable name.
     */
    public String toString() {
        return name;
    }

    /**
     * Gets the {@link FileStatus} objcet from its name (such as "Draft").
     */
    public static FileStatus parse(String name) {
        return (FileStatus)index.get(name);
    }

    static {
        for( FileStatus fs : FileStatus.values() )
            index.put(fs.name,fs);
    }
}
