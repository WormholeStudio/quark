package studio.wormhole.quark.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.utils.AccountAddressUtils;
import org.starcoin.utils.SignatureUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainAccount {
    String privateKey;
    String address;


    public AccountAddress accountAddress() {
        return AccountAddressUtils.create(this.getAddress());
    }
    public Ed25519PrivateKey ed25519PrivateKey(){
        return SignatureUtils.strToPrivateKey(this.privateKey);
    }
}
