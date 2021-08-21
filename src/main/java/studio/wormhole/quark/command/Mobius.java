package studio.wormhole.quark.command;

import picocli.CommandLine;
import studio.wormhole.quark.command.mobius.command.*;

@CommandLine.Command(name = "mobius",
        subcommands = {
                Login.class,
                GetCoin.class,
                Info.class,
                MintVoucher.class,
                MintAssets.class,
                DoAction.class,
                CommandLine.HelpCommand.class})
public class Mobius {
}
