package org.kohsuke.jnt;

/**
 * Constants for priorities of issues.
 *
 * @author Kohsuke Kawaguchi
 */
public enum Priority {
    P1(1), P2(2), P3(3), P4(4), P5(5);

    /**
     * 'n' of 'Pn' as integer.
     *
     * For example 1 for 'P1'
     */
    public final int value;

    Priority( int n ) {
        this.value = n;
    }

    /**
     * Obtains the constant by their priority value.
     *
     * The reverse operation of the {@link #value} field.
     * <code>valueOf(1)==P1</code>.
     *
     * @see #valueOf(String) 
     */
    public static Priority valueOf( int p ) {
        switch(p) {
        case 1:     return P1;
        case 2:     return P2;
        case 3:     return P3;
        case 4:     return P4;
        case 5:     return P5;
        default:
            throw new IllegalArgumentException();
        }
    }
}
