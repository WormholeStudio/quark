package studio.wormhole.quark.command.alma.airdrop;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(toBuilder = true)
@Data
public class ApiMerkleTree {
    private long airDropId;
    private int chainId;
    private String tokenType;
    private String ownerAddress;
    private String root;
    private String functionAddress;
    private List<ApiMerkleProof> proofs;
}
