package org.kohsuke.jnt.tools;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class HelpCommandlet extends Commandlet {
    public String getShortDescription() {
        return "display help about a particular command";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: help <command>");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length==0) {
            printUsage(System.err);
            return -1;
        }

        for( String command : args ) {
            Commandlet c = Commandlet.find(command);
            if(c==null) {
                System.out.println("No such command: "+command);
            } else {
                c.printUsage(System.out);
            }
        }
        
        return 0;
    }
}
