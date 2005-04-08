package org.kohsuke.jnt;

/**
 * Resolution of an issue.
 * 
 * @author Kohsuke Kawaguchi
 */
public enum IssueResolution {
    /**
     * A fix for this issue is checked into the source code repository and tested.
     */
    FIXED,
    /**
     * The problem described is not an issue.
     */
    INVALID,
    /**
     * The problem described is an issue which will never be fixed.
     */
    WONTFIX,
    /**
     * The problem described is an issue which will not be fixed in this version of the product.
     */
    LATER,
    /**
     * The problem described is an issue which will probably not be fixed in this version of the product,
     * but might still be.
     */
    REMIND,
    /**
     * The problem is a duplicate of an existing issue. Marking an issue duplicate requires
     * the issue number of the duplicating issue and will at least put that issue number in
     * the description field.
     */
    DUPLICATE,
    /**
     * All attempts at reproducing this issue were futile, reading the code produces no
     * clues as to why this behavior would occur. If more information appears later,
     * please re-assign the issue, for now, file it.
     */
    WORKSFORME
}
