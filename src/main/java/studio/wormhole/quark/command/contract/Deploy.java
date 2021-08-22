package studio.wormhole.quark.command.contract;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.types.Ed25519PublicKey;
import org.starcoin.utils.AccountAddressUtils;
import org.starcoin.utils.AuthenticationKeyUtils;
import org.starcoin.utils.Scheme;
import org.starcoin.utils.SignatureUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.helper.move.MovePackageUtil;
import studio.wormhole.quark.service.ChainService;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(name = "deploy", mixinStandardHelpOptions = true,
        exitCodeOnExecutionException = 44)
public class Deploy implements Callable<Integer> {

    @Option(names = {"--private", "-p"}, description = "private key", required = true)
    String privateKeyStr;
    @Option(names = {"--address", "-a"}, description = "address", required = true)
    String address;
    @Option(names = {"--chain",
            "-c"}, description = "chain id :localhost=254 ,main=1 ,barnard=251", defaultValue = "0", required = true)
    int chainId;
    @Option(names = {"--store",
            "-s"}, description = "project  path ,default is current ", required = false)
    String projectPath;
    @Option(names = {"--file_name",
            "-f"}, description = "file name like xxx ,if none will deploy all files ", required = false)
    String fileName;

    @Option(names = {"--code_address"}, description = "if code address is different from address ,will auto replace and compile ", required = false)
    String replaceAddress;

    @Option(names = {"--function"}, description = "format:  address::module::function_name", required = false)
    String function;

    @Option(names = {"--type_args"},
            description = "type_args,format:  address::module::struct_name split with , ",
            split = ",",
            required = false)
    List<String> type_args;
    @Option(names = {"--args"},
            description = " args  split with , if array split with whitespace ",
            split = ",", required = false)
    List<String> args;


    @Override
    public Integer call() {
        Ed25519PrivateKey privateKey = SignatureUtils.strToPrivateKey(privateKeyStr);
        Ed25519PublicKey publicKey = SignatureUtils.getPublicKey(privateKey);

        String authKey = AuthenticationKeyUtils
                .authenticationKeyED25519(Scheme.Ed25519, publicKey.value.content());
        AccountAddress gAddress = AuthenticationKeyUtils.accountAddress(authKey);
        AccountAddress pAddress = AccountAddressUtils.create(address);
        if (!gAddress.equals(pAddress)) {
            throw new RuntimeException("check address ");
        }
        if (StringUtils.isEmpty(projectPath)) {
            projectPath = new File("").getAbsolutePath();
        }

        ChainService chainService = new ChainService(ChainAccount.builder()
                .privateKey(privateKeyStr)
                .address(address)
                .build(), chainId);

        if (StringUtils.isNotEmpty(replaceAddress)) {
            MovePackageUtil.replaceAddress(projectPath, replaceAddress, address);
            MovePackageUtil.publish(projectPath);
        }

        try {
            chainService.batchDeployContract(projectPath, fileName);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (StringUtils.isNotEmpty(replaceAddress)) {
                MovePackageUtil.replaceAddress(projectPath, address, replaceAddress);
            }
        }


        if (StringUtils.isNotEmpty(function)) {
            chainService.call_function(function, type_args, args);
        }
        return 0;
    }
}
