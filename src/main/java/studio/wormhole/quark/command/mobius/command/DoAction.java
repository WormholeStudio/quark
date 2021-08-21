package studio.wormhole.quark.command.mobius.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.command.mobius.MobiusService;
import studio.wormhole.quark.command.mobius.model.Action;
import studio.wormhole.quark.command.mobius.model.CoinType;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "do_action",
        mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class DoAction implements Callable<Integer> {
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

    @CommandLine.Option(names = {"--voucher_id"},
            description = "voucher id",
            required = false)
    Long voucherId;

    @CommandLine.Option(names = {"--action"},
            description = "deposit,withdraw,borrow,repay",
            required = true)
    String action;

    @Override
    public Integer call() throws Exception {
        MobiusService mobiusService = new MobiusService(store, true);
        CoinType type = CoinType.fromString(coinType);
        mobiusService.doAction(voucherId, type, amount, Action.fromString(action));

        mobiusService.printLoginInfo();
        return null;
    }
}
