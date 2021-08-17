package studio.wormhole.quark.command;

import picocli.CommandLine;
import studio.wormhole.quark.command.alma.CancelAll;
import studio.wormhole.quark.command.alma.Claim;
import studio.wormhole.quark.command.alma.Create;

@CommandLine.Command(name = "alma",
        subcommands = {Create.class,
                Claim.class, CancelAll.class,
                CommandLine.HelpCommand.class})
public class Alma {
}
