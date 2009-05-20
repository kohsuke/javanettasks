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
import java.util.List;

/**
 * Command line interface to the java.net automation tool.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {



    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }


    public static int run(String[] _args) throws Exception {
        try {
            System.setProperty("java.net.useSystemProxies","true");
        } catch (SecurityException e) {
            // failing to set this property isn't fatal
        }

        List<String> args = Arrays.asList(_args);


        if(args.size()==0) {
            usage();
            return -1;
        }

        if(args.get(0).equals("-c")) {
            System.setProperty(".java.net",args.get(1));
            args = args.subList(2,args.size());
        }

        Commandlet command = Commandlet.find(args.get(0));
        if(command==null) {
            System.err.println("No such command: "+args.get(0));
            usage();
            return -1;
        }

        return command.run(new ConnectoinFactoryImpl(),args.subList(1,args.size()).toArray(new String[0]));
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
