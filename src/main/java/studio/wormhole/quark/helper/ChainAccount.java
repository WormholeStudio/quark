package studio.wormhole.quark.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.types.Ed25519PublicKey;
import org.starcoin.utils.AccountAddressUtils;
import org.starcoin.utils.AuthenticationKeyUtils;
import org.starcoin.utils.Scheme;
import org.starcoin.utils.SignatureUtils;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainAccount {
    String privateKey;
    String address;
    Optional<String> password;


    public String getAddress() {
        if (StringUtils.isEmpty(address)) {
            return AccountAddressUtils.hex(accountAddress());
        }
        return address;
    }

    public Optional getPassword() {
        if (password == null) {
            return Optional.empty();
        }
        return password;
    }

    public AccountAddress accountAddress() {
        if (StringUtils.isEmpty(address)) {
            Ed25519PrivateKey privateKey = SignatureUtils.strToPrivateKey(getPrivateKey());
            Ed25519PublicKey publicKey = SignatureUtils.getPublicKey(privateKey);

            String authKey = AuthenticationKeyUtils
                    .authenticationKeyED25519(Scheme.Ed25519, publicKey.value.content());
            AccountAddress gAddress = AuthenticationKeyUtils.accountAddress(authKey);
            this.address = AccountAddressUtils.hex(gAddress);
            return gAddress;
        }
        return AccountAddressUtils.create(this.getAddress());
    }

    public Ed25519PrivateKey ed25519PrivateKey() {
        return SignatureUtils.strToPrivateKey(this.privateKey);
    }
}
