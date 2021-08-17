package studio.wormhole.quark.command.alma;

import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.service.ChainService;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "cancel_all", mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class CancelAll implements Callable<Integer> {
    @CommandLine.Option(names = {"--private_key"}, description = "private key of the account which want to claim airdop ", required = true)
    String privateKeyStr;
    @CommandLine.Option(names = {"--chain",
            "-c"}, description = "chain id :localhost=254 ,main=1 ,barnard=251", defaultValue = "0", required = true)
    int chainId;


    @Override
    public Integer call() throws Exception {
        ChainService chainService = new ChainService(ChainAccount.builder()
                .privateKey(privateKeyStr)
                .build(), chainId);

        String functionAddress="0x7beb045f2dea2f7fe50ede88c3e19a72";
        chainService.call_function(functionAddress + "::MerkleDistributorScript::cancel_all_airdrop",
                Lists.newArrayList("0x00000000000000000000000000000001::STC::STC"),
                Lists.newArrayList());
        return null;
    }
}
