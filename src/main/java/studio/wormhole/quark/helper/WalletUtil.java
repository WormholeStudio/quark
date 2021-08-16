package studio.wormhole.quark.helper;

import lombok.SneakyThrows;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.starcoin.types.AccountAddress;
import org.starcoin.types.Ed25519PrivateKey;
import org.starcoin.utils.*;

import java.security.SecureRandom;


public class WalletUtil {
    static SecureRandom random = new SecureRandom();

    @SneakyThrows
    public static ChainAccount randomAccount() {
        Ed25519KeyPairGenerator ed25519KeyPairGenerator = new Ed25519KeyPairGenerator();
        ed25519KeyPairGenerator.init(new Ed25519KeyGenerationParameters(random));
        AsymmetricCipherKeyPair keyPair = ed25519KeyPairGenerator.generateKeyPair();
        Ed25519PrivateKeyParameters privateKey = (Ed25519PrivateKeyParameters) keyPair.getPrivate();

        String key = Hex.encode(privateKey.getEncoded());
        Ed25519PrivateKey pk = SignatureUtils
                .strToPrivateKey(key);
        String authKey = AuthenticationKeyUtils
                .authenticationKeyED25519(Scheme.Ed25519,
                        SignatureUtils.getPublicKey(pk).value.content());
        AccountAddress gAddress = AuthenticationKeyUtils.accountAddress(authKey);
        String stcAddress = AccountAddressUtils.hex(gAddress);

        return ChainAccount.builder()
                .address(stcAddress)
                .privateKey(key)
                .build();
    }

}
