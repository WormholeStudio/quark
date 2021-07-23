package studio.wormhole.quark.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.AccountResource;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.utils.ChainInfo;
import studio.wormhole.quark.helper.QuarkClient;
import studio.wormhole.quark.model.Txn;

@Component
public class ChainService {

  RetryTemplate template = RetryTemplate.builder()
      .infiniteRetry()
      .exponentialBackoff(TimeUnit.SECONDS.toMillis(1), 2, TimeUnit.SECONDS.toMillis(10), true)
      .build();

  public void batchDeployContract(int chainId, List<File> fileList,
      AccountAddress sender, Ed25519PrivateKey privateKey) {
    QuarkClient client = new QuarkClient(chainInfo(chainId));
    AccountResource accountResource = client.getAccountSequence(sender);
    long seqNumber = accountResource.sequence_number;
    AtomicLong seq = new AtomicLong(seqNumber);
    fileList.stream().parallel().forEach(s -> {
      String rst = client
          .deployContractPackage(sender, privateKey, s.getAbsolutePath(), seq.getAndIncrement(),
              null);
      JSONObject jsonObject = JSON.parseObject(rst);
      String txn = jsonObject.getString("result");
      Txn txnObj = Txn.builder().txn(txn).message(s.getName()).build();
      System.out.println("deploy:" + s.getName() + "," + txn);
      checkTxt(client, txnObj);
    });
  }

  @SneakyThrows
  private boolean checkTxt(QuarkClient client, Txn txn) {
    return template.execute((RetryCallback<Boolean, Throwable>) retryContext -> {
      String rst = client.getTransactionInfo(txn.getTxn());
      JSONObject jsonObject = JSON.parseObject(rst);
      JSONObject result = jsonObject.getJSONObject("result");
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
    ChainInfo chainInfo = Arrays.stream(ChainInfo.values()).filter(s -> s.getChainId() == chainId)
        .findFirst().get();
    return chainInfo;
  }
}
