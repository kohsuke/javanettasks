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

    private static final Map<String,FileStatus> index = new HashMap<String,FileStatus>();

    private final String name;

    FileStatus(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return name;
    }

    /**
     * Gets the {@link FileStatus} objcet from its name (such as "Draft").
     *
     * @throws IllegalArgumentException
     *      if the name is unrecognized.
     */
    public static FileStatus parse(String name) {
        FileStatus r = index.get(name);
        if(r==null) throw new IllegalArgumentException("Unrecognized file status: "+name);
        return r;
    }

    static {
        for( FileStatus fs : FileStatus.values() )
            index.put(fs.name,fs);
    }
}
