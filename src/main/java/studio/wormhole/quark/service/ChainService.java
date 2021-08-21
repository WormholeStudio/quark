package studio.wormhole.quark.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.novi.serde.Bytes;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.CollectionUtils;
import org.starcoin.bean.ScriptFunctionObj;
import org.starcoin.bean.TypeObj;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.Ed25519PrivateKey;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.helper.QuarkClient;
import studio.wormhole.quark.helper.move.MoveFile;
import studio.wormhole.quark.helper.move.MovePackageUtil;
import studio.wormhole.quark.abi.MoveType;
import studio.wormhole.quark.model.ChainInfo;
import studio.wormhole.quark.abi.ParamType;
import studio.wormhole.quark.model.Txn;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChainService {

    ChainAccount chainAccount;

    ChainInfo chainInfo;
    QuarkClient client;

    public AccountAddress accountAddress() {
        return chainAccount.accountAddress();
    }

    public ChainService(ChainAccount chainAccount, ChainInfo chainInfo) {
        this.chainAccount = chainAccount;
        this.chainInfo = chainInfo;
        this.client = getClient(chainInfo.getChainId(), null);
    }

    public ChainService(ChainAccount chainAccount, int chainId) {
        this.chainInfo = chainInfo(chainId);
        this.chainAccount = chainAccount;
        this.client = getClient(chainInfo.getChainId(), null);
    }

    RetryTemplate template = RetryTemplate.builder()
            .infiniteRetry()
            .exponentialBackoff(TimeUnit.SECONDS.toMillis(1), 2, TimeUnit.SECONDS.toMillis(10), true)
            .build();

    public void batchDeployContract(String projectPath, String fileName) {
        List<MoveFile> fileList = MovePackageUtil.prePackageFile(projectPath);
        if (CollectionUtils.isEmpty(fileList)) {
            throw new RuntimeException("no move files");
        }
        if (StringUtils.isNotEmpty(fileName)) {
            fileList = fileList
                    .stream()
                    .filter(s -> StringUtils.containsAny(s.getName(), fileName))
                    .collect(
                            Collectors.toList());
        }

        String rst = client
                .batchDeployContractPackage(chainAccount.accountAddress(), chainAccount.ed25519PrivateKey(), fileList, null);
        checkTxnResult(rst,
                "batch  deploy:" + fileList.stream().map(s -> s.getName()).collect(Collectors.joining(",")),
                client);
    }

    private QuarkClient getClient(int chainId, Ed25519PrivateKey privateKey) {

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
        ChainInfo chainInfo = Arrays.stream(ChainInfo.values()).filter(s -> s.getChainId() == chainId)
                .findFirst().get();
        return chainInfo;
    }


    public void call_function(ScriptFunctionObj scriptFunctionObj) {
        String rst = client.callScriptFunction(chainAccount.accountAddress(), chainAccount.ed25519PrivateKey(), scriptFunctionObj);

        System.out.println(rst);
        JSONObject json = JSON.parseObject(rst);
        if (json.containsKey("error")) {
            throw new RuntimeException(scriptFunctionObj.getFunctionName() + "," + json.getJSONObject("error").toJSONString());
        }

        checkTxnResult(rst, "call function: " + scriptFunctionObj.getFunctionName(), client);
    }

    public void dryrun_function(ScriptFunctionObj scriptFunctionObj) {
        client.dryRunScriptFunction(chainAccount.accountAddress(), chainAccount.ed25519PrivateKey(), scriptFunctionObj);
    }

    private void checkTxnResult(String rst, String message, QuarkClient client) {
        JSONObject jsonObject = JSON.parseObject(rst);
        String txn = jsonObject.getString("result");
        Txn txnObj = Txn.builder().txn(txn).message(message).build();
        System.out.println(message + "," + txn);
        checkTxt(client, txnObj);
    }

    public List<ParamType> resolveFunction(ScriptFunctionObj function) {
        String rst = client.resolveFunction(function);
        JSONObject jsonObject = JSON.parseObject(rst);
        List<ParamType> paramTypeList = jsonObject.
                getJSONObject("result")
                .getJSONArray("args").toJavaList(ParamType.class);
        return paramTypeList;
    }

    public Optional<String> call_function(String function, List<String> type_args, List<String> args) {
        List<String> param = Splitter.on("::").trimResults().splitToList(function);
        ScriptFunctionObj scriptFunctionObj = ScriptFunctionObj
                .builder()
                .moduleAddress(param.get(0))
                .moduleName(param.get(1))
                .functionName(param.get(2))
                .tyArgs(fromString(type_args))
                .args(Lists.newArrayList())
                .build();
        List<ParamType> rst = resolveFunction(scriptFunctionObj);
        boolean need_sign = need_sign(rst);
        if (need_sign) {
            List<Bytes> argsBytes = argsFromString(rst, args);
            call_function(scriptFunctionObj.toBuilder().args(argsBytes).build());
            return Optional.empty();
        }
        return Optional.ofNullable(call_contract_run(function, type_args, args));

    }

    public Optional<String> dry_call_function(String function, List<String> type_args, List<String> args) {
        List<String> param = Splitter.on("::").trimResults().splitToList(function);
        ScriptFunctionObj scriptFunctionObj = ScriptFunctionObj
                .builder()
                .moduleAddress(param.get(0))
                .moduleName(param.get(1))
                .functionName(param.get(2))
                .tyArgs(fromString(type_args))
                .args(Lists.newArrayList())
                .build();
        List<ParamType> rst = resolveFunction(scriptFunctionObj);
        List<Bytes> argsBytes = argsFromString(rst, args);
        dryrun_function(scriptFunctionObj.toBuilder().args(argsBytes).build());
        return Optional.empty();

    }

    private String call_contract_run(String function, List<String> type_args, List<String> args) {
        String rst = client.callContract(function, type_args, args);
        return rst;
    }

    private boolean need_sign(List<ParamType> rst) {
        return rst.stream().filter(p -> p.getType_tag().getMoveType() == MoveType.SINGER).findAny().isPresent();
    }

    private List<Bytes> argsFromString(List<ParamType> types, List<String> args) {
        if (CollectionUtils.isEmpty(args)) {
            return Lists.newArrayList();
        }
        List<ParamType> finalTypes = types.stream()
                .filter(s -> s.getType_tag().getMoveType() != MoveType.SINGER)
                .collect(Collectors.toList());

        return IntStream.range(0, args.size()).mapToObj(idx -> {
            String arg = args.get(idx);
            ParamType argType = finalTypes.get(idx);
            Bytes v = argType.bcsSerialize(arg);
//            System.out.println(arg + "->" + Hex.encode(v) + "," + argType);
            return v;

        }).collect(Collectors.toList());
    }

    private TypeObj fromString(String string) {
        List<String> param = Splitter.on("::").trimResults().splitToList(string);
        return
                TypeObj.builder()
                        .moduleAddress(param.get(0))
                        .moduleName(param.get(1))
                        .name(param.get(2))
                        .build();
    }

    private List<TypeObj> fromString(List<String> type_args) {
        if (CollectionUtils.isEmpty(type_args)) {
            return Lists.newArrayList();
        }
        return type_args.stream().map(this::fromString).collect(Collectors.toList());
    }

    public BigInteger getTokenScalingFactor(String tokenType) {
        String json = call_contract_run("0x1::Token::scaling_factor", Lists.newArrayList(tokenType), Lists.newArrayList());
        return JSON.parseObject(json).getJSONArray("result").getBigInteger(0);
    }

    public BigInteger toChainTokenAmount(String tokenType, String amount) {
        BigInteger scaling = getTokenScalingFactor(tokenType);
        return new BigDecimal(amount).multiply(new BigDecimal(scaling)).toBigInteger();
    }


    public void batchGetCoin(String tokenType, List<String> addressList, List<String> amount) {
        call_function("0x00000000000000000000000000000001::TransferScripts::batch_peer_to_peer_v2"
                , Lists.newArrayList(tokenType)
                , Lists.newArrayList(
                        Joiner.on(" ").join(addressList)
                        , Joiner.on(" ").join(amount)));

    }

    public void getCoin(String addressList, String token, String amount) {
        call_function("0x00000000000000000000000000000001::TransferScripts::peer_to_peer_v2", Lists.newArrayList(token), Lists.newArrayList(addressList, amount));
    }

    public void importAccount(ChainAccount chainAccount) {

        String rst = client.importAccount(chainAccount);
        System.out.println(rst);
    }

    public void changeAccount(ChainAccount chainAccount) {
        this.chainAccount = chainAccount;
    }


    public boolean isAccountExist(String address) {
        String rst = client.getAccount(address);
        JSONObject o = JSON.parseObject(rst);
        return o.getJSONObject("result") != null;
    }

    public String listResource() {
        return client.listResource(this.chainAccount);
    }

    public String toHumanReadTokenAmount(String tokenType, BigInteger amount) {
        BigInteger scaling = getTokenScalingFactor(tokenType);
        return new BigDecimal(amount.toString())
                .divide(new BigDecimal(scaling))
                .toString();

    }

    public String getResource(String address, String type) {
        return client.getResource(address,type);
    }
}
