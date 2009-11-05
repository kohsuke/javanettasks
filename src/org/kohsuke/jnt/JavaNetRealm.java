package org.kohsuke.jnt;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.AbstractList;

/**
 * Cached authentication for java.net users.
 *
 * <p>
 * A directory on a disk is used to cache users and their passwords.
 *
 * @author Kohsuke Kawaguchi
 */
public class JavaNetRealm {
    private final File root;

    public JavaNetRealm(File root) {
        this.root = root;
        root.mkdirs();
    }

    /**
     * Gets all the users known thus far.
     */
    public List<String> getAll() {
        final File[] files = root.listFiles();
        return new AbstractList<String>() {
            public String get(int index) {
                return files[index].getName();
            }

            public int size() {
                return files.length;
            }
        };
    }

    /**
     * Authenticates the user.
     */
    public boolean authenticate(String userName, String password) throws IOException {
        LOGGER.finer("Authenticating "+userName);

        String digest = getDigestOf(password);
        File auth = new File(root,userName);

        // check the local cache first.
        if (auth.exists()) {
            BufferedReader r = new BufferedReader(new FileReader(auth));
            try {
                if (digest.equals(r.readLine()))
                    return true;
            } finally {
                r.close();
            }
        }

        try {
            JavaNet.connect(userName,password);
        } catch (ProcessingException e) {
            LOGGER.log(Level.FINE, "Failed to authenticate "+userName,e);
            return false;
        }

        BufferedWriter w = new BufferedWriter(new FileWriter(auth));
        try {
            w.write(digest);
        } finally {
            w.close();
        }
        return true;
    }

    /**
     * Write-only buffer.
     */
    private static final byte[] garbage = new byte[8192];

    /**
     * Computes MD5 digest of the given input stream.
     *
     * @param source
     *      The stream will be closed by this method at the end of this method.
     * @return
     *      32-char wide string
     */
    public static String getDigestOf(InputStream source) throws IOException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            DigestInputStream in =new DigestInputStream(source,md5);
            try {
                while(in.read(garbage)>0)
                    ; // simply discard the input
            } finally {
                in.close();
            }
            return toHexString(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);    // impossible
        }
    }

    public static String getDigestOf(String text) {
        try {
            return getDigestOf(new ByteArrayInputStream(text.getBytes("UTF-8")));
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static String toHexString(byte[] data, int start, int len) {
        StringBuilder buf = new StringBuilder();
        for( int i=0; i<len; i++ ) {
            int b = data[start+i]&0xFF;
            if(b<16)    buf.append('0');
            buf.append(Integer.toHexString(b));
        }
        return buf.toString();
    }

    public static String toHexString(byte[] bytes) {
        return toHexString(bytes,0,bytes.length);
    }

    private static final Logger LOGGER = Logger.getLogger(JavaNetRealm.class.getName());
}
