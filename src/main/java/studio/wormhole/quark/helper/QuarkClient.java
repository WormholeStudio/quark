package studio.wormhole.quark.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.novi.serde.Bytes;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.ArrayUtils;
import org.starcoin.bean.ScriptFunctionObj;
import org.starcoin.types.*;
import org.starcoin.types.TransactionPayload.ScriptFunction;
import org.starcoin.utils.AccountAddressUtils;
import org.starcoin.utils.Hex;
import org.starcoin.utils.SignatureUtils;
import studio.wormhole.quark.helper.move.MoveFile;
import studio.wormhole.quark.model.ChainInfo;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
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
        String rst = response.body().string();
        return rst;
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
        RawUserTransaction rawUserTransaction = buildRawUserTransaction(sender, SignatureUtils.getPublicKey(privateKey), scriptFunction, null);
        String rst = submitHexTransaction(privateKey, rawUserTransaction);
        return rst;
    }

    public void dryRunScriptFunction(AccountAddress sender, Ed25519PrivateKey privateKey,
                                     ScriptFunctionObj scriptFunctionObj) {

        ScriptFunction scriptFunction = new ScriptFunction(scriptFunctionObj.toScriptFunction());
        buildRawUserTransaction(sender, SignatureUtils.getPublicKey(privateKey), scriptFunction, null);

    }

    public List<String> batchCallScriptFunction(AccountAddress sender, Ed25519PrivateKey privateKey,
                                                List<ScriptFunctionObj> scriptFunctionObjList) {

        long seq = getAccountSequence(sender).get().sequence_number;
        AtomicLong atomicLong = new AtomicLong(seq);
        return scriptFunctionObjList.parallelStream().map(sf -> {
            ScriptFunction scriptFunction = new ScriptFunction(sf.toScriptFunction());
            RawUserTransaction rawUserTransaction = buildRawUserTransaction(sender, SignatureUtils.getPublicKey(privateKey), scriptFunction, atomicLong.getAndIncrement());
            return submitHexTransaction(privateKey, rawUserTransaction);
        }).collect(Collectors.toList());


    }


    @SneakyThrows
    public String batchDeployContractPackage(AccountAddress sender, Ed25519PrivateKey privateKey,
                                             List<MoveFile> filePathList, ScriptFunctionObj initScriptObj) {
        org.starcoin.types.ScriptFunction sf =
                Objects.isNull(initScriptObj) ? null : initScriptObj.toScriptFunction();

        List<org.starcoin.types.Module> moduleList = filePathList.stream().map(file -> {
            byte[] contractBytes = new byte[0];
            try {
                contractBytes = Files.toByteArray(new File(file.getMvFilePath()));
            } catch (IOException e) {
                throw new RuntimeException(file.getMvFilePath() + "," + e.getLocalizedMessage());
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
        RawUserTransaction rawUserTransaction = buildRawUserTransaction(sender, SignatureUtils.getPublicKey(privateKey), payload, seqNumber);
        return submitHexTransaction(privateKey, rawUserTransaction);
    }

    @SneakyThrows
    private String dryRunHexTransaction(
            RawUserTransaction rawUserTransaction, Ed25519PublicKey publicKey) {
        List<Object> params = Lists.newArrayList(Hex.encode(rawUserTransaction.bcsSerialize()),
                Hex.encode(publicKey.value));
        String rst = call("contract.dry_run_raw", params);
        return rst;
    }

    private RawUserTransaction buildRawUserTransaction(AccountAddress sender, Ed25519PublicKey publicKey,
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
            if (this.chainId > 1 && this.chainId < 250 || this.chainId == 254) {
                ts = 0;
            }
            RawUserTransaction rawUserTransaction = new RawUserTransaction(sender, seqNumber.longValue(),
                    payload,
                    10000000L, 1L, "0x1::STC::STC",
                    ts + TimeUnit.HOURS.toSeconds(1L), chainId);
//
            String dryRunHexTransaction = dryRunHexTransaction(rawUserTransaction, publicKey);

            JSONObject result = JSON.parseObject(dryRunHexTransaction).getJSONObject("result");
            String status = result.getString("status");
            log.debug("dry_run:" + status);
            if (!"Executed".equalsIgnoreCase(status)) {
                throw new RuntimeException(result.getJSONObject("explained_status").toJSONString());
            }
            BigInteger gasUsed = result.getBigInteger("gas_used");

            rawUserTransaction = new RawUserTransaction(sender, seqNumber.longValue(),
                    payload,
                    gasUsed.longValue() * 2,
                    1L, "0x1::STC::STC",
                    ts + TimeUnit.HOURS.toSeconds(1L)
                    , chainId);

            return rawUserTransaction;
        } catch (Throwable var8) {
            throw var8;
        }
    }

    private long getChainTimestamp() {

        String rst = call("chain.info", Lists.newArrayList());
        JSONObject jsonObject = JSON.parseObject(rst);
        long ts = jsonObject.getJSONObject("result").getJSONObject("head").getLong("timestamp");
        return ts;
    }


    public String resolveFunction(ScriptFunctionObj function) {
        return call("contract.resolve_function", Lists.newArrayList(function.toRPCString()));
    }

    public String callContract(String function, List<String> type_args, List<String> args) {
        return call("contract.call_v2",
                Lists.newArrayList(
                        ImmutableMap.of("function_id", function,
                                "type_args", type_args, "args", args)
                )
        );
    }

    public String getResource(String address, String type) {
        return call("state.get_resource", Lists.newArrayList(
                address, type, ImmutableMap.of("decode", true)
        ));
    }


    public String importAccount(ChainAccount chainAccount) {
        return call("account.import", Lists.newArrayList(AccountAddressUtils.hex(chainAccount.accountAddress()), chainAccount.getPrivateKey(), chainAccount.getPassword().orElse("")));
    }

    public String getAccount(String address) {
        return call("account.get", Lists.newArrayList(address));
    }

    public String listResource(ChainAccount chainAccount) {
        return call("state.list_resource", Lists.newArrayList(chainAccount.getAddress(), ImmutableMap.of("decode", true)));
    }

//    public void createAccount(Optional<String> pwd) {
//        return;
//        call("account.import")
//    }
}
