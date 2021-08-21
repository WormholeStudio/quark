package studio.wormhole.quark.command.alma.airdrop;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@SuperBuilder(toBuilder = true)

@NoArgsConstructor
@Data
public class MerkleRecord {
    private long id;
    private long airdropId;
    private String root;
    private String userAddress;

    private ApiMerkleProof proof;
    private String txn;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)

    private Date claimAt;
}
