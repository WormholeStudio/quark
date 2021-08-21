package studio.wormhole.quark.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Component
@Command(
        subcommands = {Contract.class, Alma.class,
                Mobius.class,
                CommandLine.HelpCommand.class})
public class BaseCommand {

}
