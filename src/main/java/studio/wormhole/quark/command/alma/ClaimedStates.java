package studio.wormhole.quark.command.alma;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import studio.wormhole.quark.airdrop.ApiMerkleTree;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.service.ChainService;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "claimed_states", mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class ClaimedStates implements Callable<Integer> {

    @CommandLine.Option(names = {"--file"}, description = "json file ", required = true)
    String file;
    @CommandLine.Option(names = {"--chain",
            "-c"}, description = "chain id :localhost=254 ,main=1 ,barnard=251", defaultValue = "0", required = false)
    Integer chainId;


    @Override
    public Integer call() throws Exception {


        String json = FileUtils.readFileToString(new File(file), Charset.defaultCharset());
        ApiMerkleTree apiMerkleTree = JSON.parseObject(json, ApiMerkleTree.class);
        String functionAddress = apiMerkleTree.getFunctionAddress().toLowerCase();
        if (chainId == null) {
            chainId = apiMerkleTree.getChainId();
        }
        ChainService chainService = new ChainService(ChainAccount.builder()
                .privateKey("")
                .build(), chainId);

        apiMerkleTree.getProofs().parallelStream().forEach(s -> {
            chainService.call_function(functionAddress + "::MerkleDistributor2::is_claimed",
                    Lists.newArrayList(apiMerkleTree.getTokenType()),
                    Lists.newArrayList(
                            apiMerkleTree.getOwnerAddress(),
                            apiMerkleTree.getAirDropId() + "u64",
                            "x\"" + apiMerkleTree.getRoot().substring(2) + "\"",
                            s.getIndex() + "u64")
            ).ifPresent(v -> {

                JSONObject jsonObject = JSON.parseObject(v);
                System.out.println(s.getAddress() + "," + s.getAmount() + "," + jsonObject.getJSONArray("result").getBoolean(0));
            });
        });
        return null;
    }
}
