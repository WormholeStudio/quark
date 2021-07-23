package studio.wormhole.quark.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import studio.wormhole.quark.command.contract.Deploy;

@Command(name = "contract",
    subcommands = {Deploy.class,
        CommandLine.HelpCommand.class})
public class Contract {

}
