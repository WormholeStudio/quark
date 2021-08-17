package studio.wormhole.quark.command;

import picocli.CommandLine;
import studio.wormhole.quark.command.alma.RevokeAll;
import studio.wormhole.quark.command.alma.Claim;
import studio.wormhole.quark.command.alma.ClaimedStates;
import studio.wormhole.quark.command.alma.Create;

@CommandLine.Command(name = "alma",
        subcommands = {Create.class,
                Claim.class, RevokeAll.class, ClaimedStates.class,
                CommandLine.HelpCommand.class})
public class Alma {
}
