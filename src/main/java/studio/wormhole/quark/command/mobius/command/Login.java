package studio.wormhole.quark.command.mobius.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.command.mobius.MobiusService;
import studio.wormhole.quark.helper.ChainAccount;

import java.util.Optional;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "login",
        mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class Login implements Callable<Integer> {

    @CommandLine.Option(names = {"--private", "-p"}, description = "private key", required = true)
    String privateKeyStr;
    @CommandLine.Option(names = {"--chain"}, description = "chain id :localhost=254 ,main=1 ,barnard=251", defaultValue = "0", required = true)
    int chainId;
    @CommandLine.Option(names = {"--store"}, description = "project  path ,default is current ", required = false)
    String store;

    @Override
    public Integer call() throws Exception {
        MobiusService mobiusService = new MobiusService(store, false);
        ChainAccount account = ChainAccount.builder().privateKey(privateKeyStr).password(Optional.empty()).build();
        mobiusService.login(account, chainId);
        mobiusService.printLoginInfo();
        return null;
    }
}
