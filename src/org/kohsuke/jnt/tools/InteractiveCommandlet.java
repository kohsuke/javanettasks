package org.kohsuke.jnt.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Allows the user to interactively type multiple commands.
 *
 * @author Kohsuke Kawaguchi
 */
public class InteractiveCommandlet extends Commandlet {
    public String getShortDescription() {
        return "launches a shell where you can type multiple commands";
    }

    public void printUsage(PrintStream out) {
        out.println("Usage: interactive");
    }

    public int run(ConnectionFactory connection, String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;

        while((line=in.readLine())!=null) {
            StringTokenizer tokens = new StringTokenizer(line);
            List<String> params = new ArrayList<String>();
            while(tokens.hasMoreTokens()) {
                params.add(tokens.nextToken());
            }

            if(params.isEmpty())        continue;

            String command = params.remove(0);

            Commandlet c = Commandlet.find(command);
            if(c==null) {
                System.out.println("No such command: "+command);
            } else {
                c.run(connection,params.toArray(new String[0]));
            }
        }

        return 0;
    }
}
