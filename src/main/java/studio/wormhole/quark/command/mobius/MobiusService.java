package studio.wormhole.quark.command.mobius;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import studio.wormhole.quark.command.mobius.model.*;
import studio.wormhole.quark.helper.ChainAccount;
import studio.wormhole.quark.service.ChainService;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MobiusService {


    private ChainService chainService;
    private String store;

    private Config config;

    public MobiusService(String store, boolean login) {
        this.store = Constants.checkStore(store);
        if (login) {
            Config config = getConfig();
            chainService = new ChainService(config.getLoginAccount(), config.getChainId());
        }
    }

    @SneakyThrows
    public void login(ChainAccount chainAccount, int chainId) {
        ChainAccount richAccount = null;
        if (chainId == 254 || chainId==251) {
            richAccount = ChainAccount.builder().privateKey("0x652c5cf20ff93f5717a9ea0dff2d84df2d6afec1026c7a74ed2230afb2415bee").build();
        }
        Config config = Config.builder().chainId(chainId).loginAccount(chainAccount)
                .richAccount(richAccount)
                .contractAddress("0xf8af03dd08de49d81e4efd9e24c039cc").build();
        String json = JSON.toJSONString(config);
        FileUtils.writeStringToFile(new File(store), json, Charset.defaultCharset());
        chainService = new ChainService(config.getLoginAccount(), config.getChainId());

        if (chainId== 254){
            if (!chainService.isAccountExist(chainAccount.getAddress())) {
                System.out.println("address " + chainAccount.getAddress() + " is not exist on chain " + chainId + " ,will create it ......");
                chainService.importAccount(chainAccount);
                getCoin(CoinType.STC, "1");
            }
        }

    }

    void checkLogin(String store) {
        boolean rst = Files.exists(Paths.get(store));
        if (!rst) {
            throw new RuntimeException("please login first");
        }
    }

    public void getCoin(CoinType coinType, String amount) {
        BigInteger chainTokenAmount = chainService.toChainTokenAmount(
                coinType.getAddress(),
                amount);
        Config config = getConfig();
        switch (coinType) {
            case STC:
                chainService.changeAccount(config.getRichAccount());
                chainService.getCoin(config.getLoginAccount().getAddress(), coinType.getAddress(), chainTokenAmount.toString());
                break;
            default:
                chainService.call_function(
                        appendContract("InitializeDevScript::mint_test_token"),
                        Lists.newArrayList(coinType.getAddress()), Lists.newArrayList(chainTokenAmount.toString()));
        }


    }

    @SneakyThrows
    private Config getConfig() {
        if (this.config != null) {
            return this.config;
        }
        checkLogin(store);
        String json = FileUtils.readFileToString(new File(store), Charset.defaultCharset());
        Config config = JSON.parseObject(json, Config.class);
        this.config = config;
        return this.config;
    }

    public String appendContract(String address) {
        return this.config.getContractAddress() + "::" + address;
    }

    public void printLoginInfo() {
        System.out.println("chain:" + getConfig().getChainId());
        System.out.println("current login:" + getConfig().getLoginAccount().getAddress());
        String resource = chainService.listResource();

        printTokenBalance(resource);
        getVouchers(resource).forEach(voucher -> printVoucher(voucher));
        AssetsGallery assetsGallery = getAssets(getConfig().getLoginAccount().getAddress()).get();

        printAssetsGalley(assetsGallery);
    }

    private void printAssetsGalley(AssetsGallery assetsGallery) {
        System.out.println("Assets count:" + assetsGallery.getItems().size());
        assetsGallery.getItems().forEach(item -> {
            System.out.println("\tAssets Id:" + item.getId());
            item.getBody().getAssets().getCollateral().forEach(e -> {
                String tokenType = e.getToken_code().toAddress();
                String amount = chainService.toHumanReadTokenAmount(tokenType, e.getToken_amount());
                System.out.println("\t\t" + e.getToken_code().getName() + " 存款:" + amount + ",利息:" + e.getInterest());
            });
            item.getBody().getAssets().getDebt().forEach(e -> {
                String tokenType = e.getToken_code().toAddress();
                String amount = chainService.toHumanReadTokenAmount(tokenType, e.getToken_amount());
                System.out.println("\t\t" + e.getToken_code().getName() + " 贷款:" + amount + ",利息:" + e.getInterest());
            });
        });
    }


    private List<Voucher> getVouchers(String resource) {
        return JSON.parseObject(resource)
                .getJSONObject("result")
                .getJSONObject("resources")
                .entrySet()
                .stream()
                .filter(s -> s.getKey().startsWith("0x00000000000000000000000000000001::NFTGallery::NFTGallery"))
                .flatMap(s -> {
                    Optional<CoinType> coinType = Arrays.stream(CoinType.values())
                            .filter(t -> s.getKey().equalsIgnoreCase(voucherKey(t)))
                            .findAny();
                    if (!coinType.isPresent()) {
                        return null;
                    }
                    JSONArray jsonArray = ((JSONObject) s.getValue()).getJSONObject("json").getJSONArray("items");
                    CoinType type = coinType.get();
                    return jsonArray.stream().map(o -> Voucher.builder().jsonObject((JSONObject) o).coinType(type).build()).collect(Collectors.toList()).stream();

                }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void printVoucher(Voucher voucher) {
        String id = voucher.getJsonObject().getString("id");
        BigInteger amount = voucher.getJsonObject().getJSONObject("body").getJSONObject("tokens").getBigInteger("value");

        System.out.println("\tid:" + id + ",amount:" + chainService.toHumanReadTokenAmount(voucher.getCoinType().getAddress(), amount));
    }

    private String voucherKey(CoinType coinType) {
        String format = String.format("-::Voucher::VMeta<-::Management::StandardPosition>, -::Voucher::VBody<-::Management::StandardPosition, %s>", coinType.getAddress());

        format = format.replaceAll("-", this.config.getContractAddress());
        return "0x00000000000000000000000000000001::NFTGallery::NFTGallery<" + format + ">";
    }

    private void printTokenBalance(String resource) {
        System.out.println("钱包余额");
        JSON.parseObject(resource)
                .getJSONObject("result")
                .getJSONObject("resources")
                .entrySet()
                .stream()
                .filter(s -> s.getKey().startsWith("0x00000000000000000000000000000001::Account::Balance"))
                .filter(s -> CoinType.fromBalanceKey(s.getKey()).isPresent())
                .forEach(s -> {
                    CoinType coinType = CoinType.fromBalanceKey(s.getKey()).get();
                    BigInteger amount = ((JSONObject) s.getValue()).getJSONObject("json").getJSONObject("token").getBigInteger("value");
                    String amountString = chainService.toHumanReadTokenAmount(coinType.getAddress(), amount);
                    System.out.println("\t" + coinType.getName().toUpperCase() + ":" + amountString);
                });
    }

    public void mintVoucher(CoinType type, String amount) {
        BigInteger chainTokenAmount = chainService.toChainTokenAmount(
                type.getAddress(),
                amount);
        chainService.call_function(
                appendContract("MarketScript::mint_voucher"),
                Lists.newArrayList(type.getAddress()), Lists.newArrayList(chainTokenAmount.toString()));

    }

    public void mintAssets(long voucherId) {
        CoinType voucherType = getVoucherType(voucherId);
        chainService.call_function(
                appendContract("MarketScript::mint_assets"),
                Lists.newArrayList(voucherType.getAddress()), Lists.newArrayList(String.valueOf(voucherId)));

    }

    private CoinType getVoucherType(long voucherId) {
        String resource = chainService.listResource();
        List<Voucher> vouchers = getVouchers(resource);
        return vouchers.stream().filter(s -> String.valueOf(voucherId).equalsIgnoreCase(s.getJsonObject().getString("id"))
        ).findAny().get().getCoinType();
    }

    private Optional<AssetsGallery> getAssets(String address) {
        String type = "-::Assets2Gallery::AssetsGalleryStore<0x00000000000000000000000000000001::NFT::NFT<-::Assets2::AMeta<-::Management::StandardPosition>, -::Assets2::ABody<-::Treasury2::Assets<-::Management::StandardPosition>>>>";
        type = type.replaceAll("-", this.config.getContractAddress());
        String rst = chainService.getResource(address, type);
        JSONObject jsonObject = JSON.parseObject(rst);
        if (jsonObject.getJSONObject("result") == null) {
            return Optional.empty();
        }
        JSONObject object = jsonObject.getJSONObject("result").getJSONObject("json");
        if (object.containsKey("items") && object.getJSONObject("items").containsKey("vec")) {
            JSONArray array = object.getJSONObject("items").getJSONArray("vec");
            if (array.size() == 0) {
                return Optional.empty();
            }
            List<ChainAssets> list = array.getJSONArray(0).toJavaList(ChainAssets.class);
            return Optional.of(AssetsGallery.builder().items(list).build());
        }
        return Optional.empty();
    }


    public void doAction(Long voucherId, CoinType type, String amount, Action action) {
        if (voucherId == null) {
            voucherId = getAssets(getConfig().getLoginAccount().getAddress()).get().getItems().get(0).getId();
        }
        BigInteger chainTokenAmount = chainService.toChainTokenAmount(
                type.getAddress(),
                amount);
        chainService.call_function(
                appendContract(action.getFunction()),
                Lists.newArrayList(type.getAddress()), Lists.newArrayList(voucherId.toString(), chainTokenAmount.toString()));

    }
}

