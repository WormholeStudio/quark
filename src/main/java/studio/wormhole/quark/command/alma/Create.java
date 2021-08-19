package studio.wormhole.quark.command.alma;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.starcoin.utils.AccountAddressUtils;
import picocli.CommandLine;
import studio.wormhole.quark.airdrop.ApiMerkleTree;
import studio.wormhole.quark.airdrop.CSVRecord;
import studio.wormhole.quark.airdrop.MerkleTreeHelper;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.service.ChainService;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "create", mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class Create implements Callable<Integer> {

    @CommandLine.Option(names = {"--private_key"}, description = "private key", required = true)
    String privateKeyStr;

    @CommandLine.Option(names = {"--file"}, description = "csv file address", required = true)
    String file;
    @CommandLine.Option(names = {"--chain",
            "-c"}, description = "chain id :localhost=254 ,main=1 ,barnard=251", defaultValue = "0", required = true)
    int chainId;
    @CommandLine.Option(names = {"--function_address"}, description = "function address", required = false)
    String functionAddress;
    @CommandLine.Option(names = {"--token_type"}, description = "token type", required = false)
    String token_type;
    @CommandLine.Option(names = {"--out"}, description = "result json file path", required = false)
    String out;
    @CommandLine.Option(names = {"--airdrop_id"}, description = "aridrop_id", required = false)
    Long airdropId;

    @Override
    public Integer call() throws Exception {
        ChainService chainService = new ChainService(ChainAccount.builder()
                .privateKey(privateKeyStr)
                .build(), chainId);
        CsvToBean<CSVRecord> csvToBean = new CsvToBeanBuilder(
                new InputStreamReader(new FileInputStream(file)))
                .withType(CSVRecord.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        List<CSVRecord> records = Lists.newArrayList(csvToBean.iterator());


        if (airdropId == null) {
            airdropId = System.currentTimeMillis();
        }
        ApiMerkleTree apiMerkleTree = MerkleTreeHelper.merkleTree(airdropId, records);

        apiMerkleTree.setAirDropId(airdropId);
        if (StringUtils.isEmpty(functionAddress)) {
            functionAddress = "0xb987F1aB0D7879b2aB421b98f96eFb44";
        }
        apiMerkleTree.setFunctionAddress(functionAddress);
        if (StringUtils.isEmpty(token_type)) {
            token_type = "0x00000000000000000000000000000001::STC::STC";
        }
        apiMerkleTree.setTokenType(token_type);
        apiMerkleTree.setOwnerAddress(AccountAddressUtils.hex(chainService.accountAddress()));

        apiMerkleTree.setChainId(chainId);
        System.out.println(JSON.toJSONString(apiMerkleTree, true));
        if (StringUtils.isNotEmpty(out)) {
            FileUtils.writeStringToFile(new File(out), JSON.toJSONString(apiMerkleTree, true), Charset.defaultCharset());
        }
        BigInteger amount = apiMerkleTree.getProofs().stream().map(s -> s.getAmount())
                .reduce((bigInteger, bigInteger2) -> bigInteger.add(bigInteger2)).get();

        chainService.call_function(apiMerkleTree.getFunctionAddress() + "::MerkleDistributorScript::create",
                Lists.newArrayList(apiMerkleTree.getTokenType()),
                Lists.newArrayList(apiMerkleTree.getAirDropId() + "", apiMerkleTree.getRoot(), amount.toString(), apiMerkleTree.getProofs().size() + "")
        );
        return null;
    }
}
