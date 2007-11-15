package org.kohsuke.jnt.tools;

import java.io.PrintStream;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Commandlet {
    public static final Commandlet[] ALL = {
        new HelpCommandlet(),
        new MkdirCommandlet(),
        new UploadFileCommandlet(),
        new LinkUrlCommandlet(),
        new GrantRoleCommandlet(),
        new CopySubscribersCommandlet(),
        new DeclineRoleCommandlet(),
        new ProcessRoleCommandlet(),
        new ListMyProjectsCommandlet(),
        new ListSubProjectsCommandlet(),
        new ListUserCommandlet(),
        new SubscribeListCommandlet(),
        new SubscribersCommandlet(),
        new InteractiveCommandlet(),
        new ApproveProjectCommandlet(),
        new MoveProjectCommandlet(),
        new InstallAutoResponderCommandlet(),
        new UnsubscribeAllCommandlet(),
        new AnnounceCommandlet(),
        new AdhocQueryCommandlet()
    };


    public final String getCommandName() {
        String name = getClass().getName();

        // get the short name
        int idx = name.lastIndexOf('.');
        name = name.substring(idx+1);

        // remove trailing "commandlet"
        name = name.substring(0,name.length()-"commandlet".length());

        // change the first char to the lower case
        name = Character.toLowerCase(name.charAt(0))+name.substring(1);

        return name;
    }

    public abstract String getShortDescription();

    public abstract void printUsage(PrintStream out);

    public abstract int run(ConnectionFactory connection, String[] args) throws Exception;



    public static Commandlet find(String name) {
        for (Commandlet c : ALL) {
            if(c.getCommandName().equals(name))
                return c;
        }
        return null;
    }

}
