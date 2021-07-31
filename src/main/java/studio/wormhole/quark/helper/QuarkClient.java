package studio.wormhole.quark.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.novi.serde.Bytes;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.starcoin.bean.ScriptFunctionObj;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.AccountResource;
import org.starcoin.types.ChainId;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.types.Module;
import org.starcoin.types.RawUserTransaction;
import org.starcoin.types.SignedUserTransaction;
import org.starcoin.types.TransactionPayload;
import org.starcoin.types.TransactionPayload.ScriptFunction;
import org.starcoin.utils.AccountAddressUtils;
import org.starcoin.utils.ChainInfo;
import org.starcoin.utils.Hex;
import org.starcoin.utils.SignatureUtils;

public class QuarkClient {

  private ChainInfo chainInfo;
  public static final MediaType JSON_MEDIA_TYPE = MediaType.parse(
      "application/json; charset=utf-8");
  private final String baseUrl;
  private final int chainId;
  private OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

  public QuarkClient(ChainInfo chainInfo) {
    this.chainInfo = chainInfo;
    this.baseUrl = chainInfo.getUrl();
    this.chainId = chainInfo.getChainId();
  }

  public QuarkClient(String baseUrl, int chainId) {
    this.baseUrl = baseUrl;
    this.chainId = chainId;
  }

  @SneakyThrows
  private String call(String method, List<Object> params) {
    JSONObject jsonBody = new JSONObject();
    jsonBody.put("jsonrpc", "2.0");
    jsonBody.put("method", method);
    jsonBody.put("id", UUID.randomUUID().toString());
    jsonBody.put("params", params);
    RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonBody.toString());
    Request request = new Request.Builder().post(body).url(this.baseUrl).build();
    Response response = okHttpClient.newCall(request).execute();
    return response.body().string();
  }

  @SneakyThrows
  //  @TODO 链上改了返回结构以后要修改
  public Optional<AccountResource> getAccountSequence(AccountAddress sender) {
    String path = AccountAddressUtils.hex(
        sender) + "/1/0x00000000000000000000000000000001::Account::Account";
    String rst = call("state.get", Lists.newArrayList(path));
    JSONObject jsonObject = JSON.parseObject(rst);
    if (jsonObject.get("result") == null) {
      return Optional.empty();
    }
    List<Byte> result = jsonObject
        .getJSONArray("result")
        .toJavaList(Byte.class);
    Byte[] bytes = result.toArray(new Byte[0]);
    return Optional.of(AccountResource.bcsDeserialize(ArrayUtils.toPrimitive(bytes)));
  }


  public String getTransactionInfo(String txn) {
    return call("chain.get_transaction_info", Lists.newArrayList(txn));
  }

  public String callScriptFunction(AccountAddress sender, Ed25519PrivateKey privateKey,
      ScriptFunctionObj scriptFunctionObj) {
    ScriptFunction scriptFunction = new ScriptFunction(scriptFunctionObj.toScriptFunction());
    RawUserTransaction rawUserTransaction = buildRawUserTransaction(sender, scriptFunction, null);
    String rst = submitHexTransaction(privateKey, rawUserTransaction);
    return rst;
  }


  @SneakyThrows
  public String deployContractPackage(AccountAddress sender, Ed25519PrivateKey privateKey,
      String filePath, Long seqNumber, ScriptFunctionObj initScriptObj) {
    org.starcoin.types.ScriptFunction sf =
        Objects.isNull(initScriptObj) ? null : initScriptObj.toScriptFunction();
    byte[] contractBytes = Files.toByteArray(new File(filePath));
    Module module = new Module(new Bytes(contractBytes));
    org.starcoin.types.Package contractPackage = new org.starcoin.types.Package(sender,
        Lists.newArrayList(
            module),
        Optional.ofNullable(sf));
    TransactionPayload.Package.Builder builder = new TransactionPayload.Package.Builder();
    builder.value = contractPackage;
    TransactionPayload payload = builder.build();
    return submitTransaction(sender, privateKey, payload, seqNumber);
  }

  @SneakyThrows
  public String batchDeployContractPackage(AccountAddress sender, Ed25519PrivateKey privateKey,
      List<MoveFile> filePathList, ScriptFunctionObj initScriptObj) {
    org.starcoin.types.ScriptFunction sf =
        Objects.isNull(initScriptObj) ? null : initScriptObj.toScriptFunction();

    List<Module> moduleList = filePathList.stream().map(file -> {
      byte[] contractBytes = new byte[0];
      try {
        contractBytes = Files.toByteArray(new File(file.getMvFilePath()));
      } catch (IOException e) {
        e.printStackTrace();
      }
      Module module = new Module(new Bytes(contractBytes));
      return module;
    }).collect(Collectors.toList());
    org.starcoin.types.Package contractPackage = new org.starcoin.types.Package(sender,
        moduleList,
        Optional.ofNullable(sf));
    TransactionPayload.Package.Builder builder = new TransactionPayload.Package.Builder();
    builder.value = contractPackage;
    TransactionPayload payload = builder.build();

    Optional<AccountResource> accountResource = getAccountSequence(sender);
    long seqNumber = accountResource.isPresent() ? accountResource.get().sequence_number : 1L;
    return submitTransaction(sender, privateKey, payload, seqNumber);
  }

  @SneakyThrows
  public String submitHexTransaction(Ed25519PrivateKey privateKey,
      RawUserTransaction rawUserTransaction) {
    SignedUserTransaction signedUserTransaction = SignatureUtils.signTxn(privateKey,
        rawUserTransaction);
    List<Object> params = Lists.newArrayList(Hex.encode(signedUserTransaction.bcsSerialize()));
    return call("txpool.submit_hex_transaction", params);
  }

  public String submitTransaction(AccountAddress sender, Ed25519PrivateKey privateKey,
      TransactionPayload payload, Long seqNumber) {
    RawUserTransaction rawUserTransaction = buildRawUserTransaction(sender, payload, seqNumber);
    return submitHexTransaction(privateKey, rawUserTransaction);
  }

  private RawUserTransaction buildRawUserTransaction(AccountAddress sender,
      TransactionPayload payload, Long seqNumber) {
    try {
      if (seqNumber == null) {
        Optional<AccountResource> accountResource = getAccountSequence(sender);
        if (accountResource.isPresent()) {
          seqNumber = accountResource.get().sequence_number;
        } else {
          seqNumber = 1L;
        }

      }
      ChainId chainId = new ChainId((byte) this.chainId);
      long ts = System.currentTimeMillis() / 1000L;
      if (this.chainId == 254) {
        ts = 1;
      }
      RawUserTransaction rawUserTransaction = new RawUserTransaction(sender, seqNumber.longValue(),
          payload,
          10000000L, 1L, "0x1::STC::STC",
          ts + TimeUnit.HOURS.toSeconds(1L), chainId);
      return rawUserTransaction;
    } catch (Throwable var8) {
      throw var8;
    }
  }


  public String resolveFunction(ScriptFunctionObj function) {
    return call("contract.resolve_function", Lists.newArrayList(function.toRPCString()));
  }

}
