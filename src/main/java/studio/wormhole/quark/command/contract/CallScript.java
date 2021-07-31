package studio.wormhole.quark.command.contract;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.starcoin.bean.ScriptFunctionObj;
import org.starcoin.bean.TypeObj;
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
  @Option(names = {"--private", "-p"}, description = "private key", required = true)
  String privateKeyStr;
  @Option(names = {"--address", "-a"}, description = "private key", required = true)
  String address;

  @Autowired
  private ChainService chainService;

  @Override
  public Integer call() throws Exception {
    Ed25519PrivateKey privateKey = SignatureUtils.strToPrivateKey(privateKeyStr);
    Ed25519PublicKey publicKey = SignatureUtils.getPublicKey(privateKey);

    String authKey = AuthenticationKeyUtils
        .authenticationKeyED25519(Scheme.Ed25519, publicKey.value.content());
    AccountAddress gAddress = AuthenticationKeyUtils.accountAddress(authKey);
    AccountAddress pAddress = AccountAddressUtils.create(address);
    if (!gAddress.equals(pAddress)) {
      throw new RuntimeException("check address ");
    }
    List<String> param = Splitter.on("::").trimResults().splitToList(function);
    ScriptFunctionObj scriptFunctionObj = ScriptFunctionObj
        .builder()
        .moduleAddress(param.get(0))
        .moduleName(param.get(1))
        .functionName(param.get(2))
        .tyArgs(Lists.newArrayList(TypeObj.STC()))
        .args(Lists.newArrayList()
        )
        .build();

    String rst = chainService.resolveFunction(chainId, scriptFunctionObj);
    JSON obj = JSON.parseObject(rst);
    System.out.println(obj);
//    chainService.call_function(chainId, pAddress, privateKey, scriptFunctionObj);
    return 1;
  }
}
