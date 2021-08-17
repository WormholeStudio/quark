package studio.wormhole.quark.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ChainInfo {
    DEV("local", "http://127.0.0.1:9850", 254),
    HALLEY("halley", "https://halley-seed.starcoin.org", 253),
    BARNARD("barnard", "https://barnard-seed.starcoin.org", 251),
    MAIN("main", "https://main-seed.starcoin.org", 1);

    private String name;
    private String url;
    private int chainId;


}
