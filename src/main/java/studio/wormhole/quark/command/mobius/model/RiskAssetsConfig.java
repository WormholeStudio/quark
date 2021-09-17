package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RiskAssetsConfig {
    BigInteger liquidationThreshold;
}
