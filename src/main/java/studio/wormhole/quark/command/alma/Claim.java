package studio.wormhole.quark.command.alma;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.starcoin.utils.AccountAddressUtils;
import picocli.CommandLine;
import studio.wormhole.quark.airdrop.ApiMerkleProof;
import studio.wormhole.quark.airdrop.ApiMerkleTree;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.service.ChainService;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "claim", mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class Claim implements Callable<Integer> {

    @CommandLine.Option(names = {"--private_key"}, description = "private key of the account which want to claim airdop ", required = true)
    String privateKeyStr;

    @CommandLine.Option(names = {"--file"}, description = "csv file address", required = true)
    String file;
    @CommandLine.Option(names = {"--chain",
            "-c"}, description = "chain id :localhost=254 ,main=1 ,barnard=251", defaultValue = "0", required = true)
    int chainId;


    @Override
    public Integer call() throws Exception {
        ChainService chainService = new ChainService(ChainAccount.builder()
                .privateKey(privateKeyStr)
                .build(), chainId);

        String json = FileUtils.readFileToString(new File(file), Charset.defaultCharset());
        ApiMerkleTree apiMerkleTree = JSON.parseObject(json, ApiMerkleTree.class);
        ApiMerkleProof proof = apiMerkleTree.getProofs().stream().filter(s -> StringUtils.equalsIgnoreCase(s.getAddress(), AccountAddressUtils.hex(chainService.accountAddress())))
                .findAny().orElseThrow(() -> new RuntimeException("no such address"));
        String functionAddress = apiMerkleTree.getFunctionAddress().toLowerCase();

        chainService.call_function(functionAddress + "::MerkleDistributorScript::claim_script",
                Lists.newArrayList(apiMerkleTree.getTokenType()),
                Lists.newArrayList(apiMerkleTree.getOwnerAddress(),
                        apiMerkleTree.getAirDropId() + "",
                        apiMerkleTree.getRoot(),
                        proof.getIndex() + "", proof.getAmount().toString(),
                        Joiner.on(" ").join(proof.getProof())
                ));
        return null;
    }
}
