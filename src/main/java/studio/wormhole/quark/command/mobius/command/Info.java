package studio.wormhole.quark.command.mobius.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.command.mobius.MobiusService;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "info",
        mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class Info implements Callable<Integer> {
    @CommandLine.Option(names = {"--store"}, description = "project  path  ", required = false)
    String store;

    @Override
    public Integer call() throws Exception {
        MobiusService mobiusService = new MobiusService(store, true);

        mobiusService.printLoginInfo();
        return null;
    }
}
