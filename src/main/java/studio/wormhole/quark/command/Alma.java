package studio.wormhole.quark.command;

import picocli.CommandLine;
import studio.wormhole.quark.command.alma.*;

@CommandLine.Command(name = "alma",
        subcommands = {Create.class,
                Revoke.class,
                Claim.class, RevokeAll.class, ClaimedStates.class,
                CommandLine.HelpCommand.class})
public class Alma {
}
