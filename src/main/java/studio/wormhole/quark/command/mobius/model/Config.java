package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import studio.wormhole.quark.helper.ChainAccount;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Config {
    private int chainId;
    private ChainAccount loginAccount;
    private ChainAccount richAccount;
    private String contractAddress;
}
