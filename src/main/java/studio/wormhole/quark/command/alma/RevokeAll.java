package studio.wormhole.quark.command.alma;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.service.ChainService;

import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "revoke", mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class RevokeAll implements Callable<Integer> {
    @CommandLine.Option(names = {"--private_key"}, description = "private key of the account which want to claim airdop ", required = true)
    String privateKeyStr;
    @CommandLine.Option(names = {"--chain"}, description = "chain id :localhost=254 ,main=1 ,barnard=251", defaultValue = "0", required = true)
    int chainId;
    @CommandLine.Option(names = {"--token_type"}, description = "token type", required = false)
    String token_type;
    @CommandLine.Option(names = {"--function_address"}, description = "function address", required = false)
    String functionAddress;
    @CommandLine.Option(names = {"--airdrop_id"}, description = "function address", required = false)
    String airdropId;

    @Override
    public Integer call() {
        ChainService chainService = new ChainService(ChainAccount.builder()
                .privateKey(privateKeyStr)
                .build(), chainId);
        if (StringUtils.isEmpty(functionAddress)) {
            functionAddress = "0xb987F1aB0D7879b2aB421b98f96eFb44";
        }
        if (StringUtils.isEmpty(token_type)) {
            token_type = "0x00000000000000000000000000000001::STC::STC";
        }
        if (StringUtils.isNotEmpty(airdropId)) {
            chainService.call_function(functionAddress + "::MerkleDistributorScript::revoke_airdrop",
                    Lists.newArrayList(token_type),
                    Lists.newArrayList(airdropId));
        } else {
            chainService.call_function(functionAddress + "::MerkleDistributorScript::revoke_all",
                    Lists.newArrayList(token_type),
                    Lists.newArrayList());
        }

        return null;
    }
}
