package studio.wormhole.quark.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.starcoin.bean.ScriptFunctionObj;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.utils.ChainInfo;
import studio.wormhole.quark.helper.MoveFile;
import studio.wormhole.quark.helper.QuarkClient;
import studio.wormhole.quark.model.Txn;

@Component
public class ChainService {

  RetryTemplate template = RetryTemplate.builder()
      .infiniteRetry()
      .exponentialBackoff(TimeUnit.SECONDS.toMillis(1), 2, TimeUnit.SECONDS.toMillis(10), true)
      .build();

  public void batchDeployContract(int chainId, List<MoveFile> fileList,
      AccountAddress sender, Ed25519PrivateKey privateKey) {
    QuarkClient client = getClient(chainId, privateKey);
    String rst = client
        .batchDeployContractPackage(sender, privateKey, fileList, null);
    checkTxnResult(rst,
        "batch  deploy:" + fileList.stream().map(s -> s.getName()).collect(Collectors.joining(",")),
        client);
  }

  private QuarkClient getClient(int chainId, Ed25519PrivateKey privateKey) {
    if (chainId == 254) {
      QuarkClient client = new QuarkClient("http://127.0.0.1:9850", 254);

      return client;
    }
    return new QuarkClient(chainInfo(chainId));
  }

  @SneakyThrows
  private boolean checkTxt(QuarkClient client, Txn txn) {
    return template.execute((RetryCallback<Boolean, Throwable>) retryContext -> {
      String rst = client.getTransactionInfo(txn.getTxn());
      JSONObject jsonObject = JSON.parseObject(rst);
      JSONObject result = jsonObject.getJSONObject("result");
      JSONObject error = jsonObject.getJSONObject("error");
      if (error != null) {
        System.out.println(error.toString());
        return true;
      }
      if (result != null) {
        String message = txn.getTxn() + "," + txn.getMessage() + "," + result.getString("status");
        System.out.println(message);
        return true;
      }
      System.out.println(txn.getMessage() + " ...waiting....");
      throw new RuntimeException("");
    });

  }


  private ChainInfo chainInfo(int chainId) {
//    if (chainId == 1) {
//      throw new RuntimeException("don't use this tool on main chain");
//    }
    ChainInfo chainInfo = Arrays.stream(ChainInfo.values()).filter(s -> s.getChainId() == chainId)
        .findFirst().get();
    return chainInfo;
  }


  public void call_function(int chainId,
      AccountAddress sender, Ed25519PrivateKey privateKey
      , ScriptFunctionObj scriptFunctionObj) {
    QuarkClient client = getClient(chainId, privateKey);
    String rst = client.callScriptFunction(sender, privateKey, scriptFunctionObj);
    checkTxnResult(rst, "call function " + scriptFunctionObj.toString(), client);
  }

  private void checkTxnResult(String rst, String message, QuarkClient client) {
    JSONObject jsonObject = JSON.parseObject(rst);
    String txn = jsonObject.getString("result");
    Txn txnObj = Txn.builder().txn(txn).message(message).build();
    System.out.println(message + "," + txn);
    checkTxt(client, txnObj);
  }

  public String resolveFunction(int chainId, ScriptFunctionObj function) {
    QuarkClient client = getClient(chainId, null);
    ;
    return client.resolveFunction(function);
  }

}
