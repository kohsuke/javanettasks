package org.kohsuke.jnt.tools;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class AdhocQueryCommandlet extends Commandlet {
    public String getShortDescription() {
        return "Runs ad-hoc query against database. Requires domain access privilege";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: adhocQuery <SQL>");
        out.println("Run ad-hoc query against database.");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        if(args.length!=1) {
            printUsage(System.err);
            return -1;
        }

        String[][] data = connection.connect().runAdHocQuery(args[0]);
        for (String[] row : data) {
            StringBuilder buf = new StringBuilder();
            for (String cell : row) {
                if(buf.length()>0)  buf.append(',');
                buf.append('"');
                buf.append(cell.replace("\"","\"\""));
                buf.append('"');
            }
            System.out.println(buf);
        }
        return 0;
    }
}

