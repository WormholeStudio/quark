package studio.wormhole.quark.command.mobius.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.command.mobius.MobiusService;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "mint-assets",
        mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class MintAssets implements Callable<Integer> {
    @CommandLine.Option(names = {"--store"}, description = "project  path  ", required = false)
    String store;
    @CommandLine.Option(names = {"--voucher_id"},
            description = "voucher id",
            required = true)
    long voucherId;


    @Override
    public Integer call() throws Exception {
        MobiusService mobiusService = new MobiusService(store, true);
        mobiusService.mintAssets(voucherId);
        return null;
    }
}
