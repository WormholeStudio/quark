package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CoinType {
    //
    STC("STC", "0x00000000000000000000000000000001::STC::STC", "0x1::STCUSDOracle::STCUSD"),
    MBTC("MBTC", "0x4fe7BBbFcd97987b966415F01995a229::MBTC::MBTC", "0x07fa08a855753f0ff7292fdcbe871216::BTC_USD::BTC_USD"),
    METH("METH", "0x4fe7BBbFcd97987b966415F01995a229::METH::METH", "0x07fa08a855753f0ff7292fdcbe871216::ETH_USD::ETH_USD"),
    MUSDT("MUSDT", "0x4fe7BBbFcd97987b966415F01995a229::MUSDT::MUSDT", ""),

    ;
    private String name;
    private String address;

    private String oracleLabel;

    public static CoinType fromString(String coinType) {
        return Arrays.stream(CoinType.values()).filter(s -> s.getName().equalsIgnoreCase(coinType))
                .findAny().orElseThrow(() -> new RuntimeException("not support coin type"));
    }

    public static CoinType fromTokenCode(String tokenCode) {
        return Arrays.stream(CoinType.values()).filter(s -> s.getAddress().equalsIgnoreCase(tokenCode))
                .findAny().orElseThrow(() -> new RuntimeException("not support coin type"));

    }

    public static Optional<CoinType> fromBalanceKey(String key) {
        return Arrays.stream(CoinType.values()).filter(s -> s.balanceKey().equalsIgnoreCase(key))
                .findAny();
    }

    public String balanceKey() {
        return "0x00000000000000000000000000000001::Account::Balance<" + this.getAddress() + ">";
    }
}
