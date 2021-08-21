package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CoinType {

    STC("STC", "0x00000000000000000000000000000001::STC::STC"),
    MBTC("MBTC", "0xf8af03dd08de49d81e4efd9e24c039cc::MBTC::MBTC"),
    METH("METH", "0xf8af03dd08de49d81e4efd9e24c039cc::METH::METH"),
    MUSDT("MUSDT", "0xf8af03dd08de49d81e4efd9e24c039cc::MUSDT::MUSDT"),

    ;
    private String name;
    private String address;

    public static CoinType fromString(String coinType) {
        return Arrays.stream(CoinType.values()).filter(s -> s.getName().equalsIgnoreCase(coinType))
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
