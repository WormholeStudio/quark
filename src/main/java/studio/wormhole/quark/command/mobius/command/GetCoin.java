package studio.wormhole.quark.command.mobius.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.command.mobius.model.CoinType;
import studio.wormhole.quark.command.mobius.MobiusService;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "get-coin",
        mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class GetCoin implements Callable<Integer> {
    @CommandLine.Option(names = {"--store"}, description = "project  path  ", required = false)
    String store;
    @CommandLine.Option(names = {"--coin"},
            description = "coin like stc,mbtc,meth,musdt ",
            required = true)
    String coinType;
    @CommandLine.Option(names = {"--amount"},
            description = "human readable amount like 4.567",
            required = true)
    String amount;

    @Override
    public Integer call() throws Exception {
        MobiusService mobiusService = new MobiusService(store,true);
        CoinType type = CoinType.fromString(coinType);
        mobiusService.getCoin(type,amount);
        return null;
    }
}
