/*
 * Created on Aug 6, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt.tools;

import org.kohsuke.jnt.JavaNet;
import org.kohsuke.jnt.ProcessingException;

import java.util.Arrays;

/**
 * Command line interface to the java.net automation tool.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {



    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }


    public static int run(String[] args) throws Exception {
        try {
            System.setProperty("java.net.useSystemProxies","true");
        } catch (SecurityException e) {
            // failing to set this property isn't fatal
        }


        if(args.length==0) {
            usage();
            return -1;
        }

        Commandlet command = Commandlet.find(args[0]);
        if(command==null) {
            System.err.println("No such command: "+args[0]);
            usage();
            return -1;
        }

        args = Arrays.asList(args).subList(1,args.length).toArray(new String[0]);

        return command.run(new ConnectoinFactoryImpl(),args);
    }

    private static void usage() {
        System.err.println("Usage: java -jar javanettasks.jar <command>");
        System.err.println("where command can be ...");
        for (Commandlet c : Commandlet.ALL) {
            System.err.println("  "+c.getCommandName());
            System.err.println("    "+c.getShortDescription());
        }
        System.exit(1);
    }

    private static final class ConnectoinFactoryImpl implements ConnectionFactory {
        private JavaNet javaNet;
        public JavaNet connect() throws ProcessingException {
            if(javaNet==null)
                javaNet = JavaNet.connect();
            return javaNet;
        }
    }
}
