package studio.wormhole.quark.command.contract;

import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.types.Ed25519PublicKey;
import org.starcoin.utils.AuthenticationKeyUtils;
import org.starcoin.utils.Scheme;
import org.starcoin.utils.SignatureUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import studio.wormhole.quark.service.ChainService;

@Component
@Command(name = "call_script_function",
    mixinStandardHelpOptions = true,
    exitCodeOnExecutionException = 44)
public class CallScript implements Callable<Integer> {

  @Option(names = {"--chain",
      "-c"}, description = "chain id :localhost=0 ,main=1 ,barnard=251", defaultValue = "0", required = true)
  int chainId;

  @Option(names = {"--function",
      "-f"}, description = "format:  address::module::function_name", required = true)
  String function;
  @Option(names = {"--private", "-p"}, description = "private key,", required = true)
  String privateKeyStr;

  @Option(names = {"--type_args", "-ta"},
      description = "type_args,format:  address::module::struct_name split with , ",
      split = ",",
      required = false)
  List<String> type_args;
  @Option(names = {"--args", "-ag"},
      description = " args  split with , if array split with whitespace ",
      split = ",", required = false)
  List<String> args;

  @Autowired
  private ChainService chainService;

  @Override
  public Integer call() {
    Ed25519PrivateKey privateKey = SignatureUtils.strToPrivateKey(privateKeyStr);
    Ed25519PublicKey publicKey = SignatureUtils.getPublicKey(privateKey);
    String authKey = AuthenticationKeyUtils
        .authenticationKeyED25519(Scheme.Ed25519, publicKey.value.content());
    AccountAddress gAddress = AuthenticationKeyUtils.accountAddress(authKey);
    chainService.call_function(chainId, gAddress, privateKey, function, type_args, args);
    return 1;
  }


}
