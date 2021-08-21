package studio.wormhole.quark.command.alma.airdrop;

import lombok.*;

import java.math.BigInteger;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(toBuilder = true)
@Data
public class ApiMerkleProof {
  private String address;
  private long index;
  private BigInteger amount;
  private List<String> proof;
}
