package studio.wormhole.quark.command.contract;

import com.google.common.collect.Streams;
import com.google.common.io.Files;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import studio.wormhole.quark.service.ChainService;

@Component
@Command(name = "deploy", mixinStandardHelpOptions = true,
    exitCodeOnExecutionException = 44)
public class Deploy implements Callable<Integer> {

  @Autowired
  private ChainService chainService;
  @Option(names = {"--private", "-p"}, description = "private key", required = true)
  String privateKeyStr;
  @Option(names = {"--address", "-a"}, description = "private key", required = true)
  String address;
  @Option(names = {"--chain",
      "-c"}, description = "chain id :localhost=0 ,main=1 ,barnard=251", defaultValue = "0", required = true)
  int chainId;
  @Option(names = {"--storage",
      "-s"}, description = "storage path ,default is current ", required = false)
  String storagePath;

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
    if (StringUtils.isEmpty(storagePath)) {
      storagePath = new File("").getAbsolutePath();
    }
    Iterable<File> fileIterable = Files
        .fileTraverser()
        .breadthFirst(new File(storagePath));
    List<File> fileList = Streams.stream(fileIterable)
        .filter(s -> StringUtils
            .containsAnyIgnoreCase(s.getAbsolutePath(), address))
        .filter(s -> s.isFile())
        .filter(s -> s.getPath().endsWith(".mv"))
        .sorted()
        .collect(Collectors.toList());
    chainService.batchDeployContract(chainId, fileList, pAddress, privateKey);
    return 0;
  }
}
