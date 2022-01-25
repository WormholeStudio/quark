package studio.wormhole.quark.helper;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.Bech32;
import org.starcoin.types.AccountAddress;
import org.starcoin.utils.AccountAddressUtils;

public class IdentifierUtil {

  public static final byte VERSION_1 = 1;

  //stc1pz6l5m6jlgpgfnsycm42hsmdg7303p5nqmk8w8366nntec09nqft3d06daf05q5yeczvd64tcdk50gpsc628
//  0x16bf4dea5f405099c098dd55786da8f4
  public static String identifierToAddress(String identifier) {
    Bech32.Bech32Data data = Bech32.decode(identifier);
    if (!"stc".equalsIgnoreCase(data.hrp)) {
      throw new IllegalArgumentException(String.format("Invalid network prefix : %s != %s", "stc", data.hrp));
    }

    byte version = data.data[0];
    if (version != VERSION_1) {
      throw new IllegalArgumentException(String.format("unknown account identifier format version: $d", version));
    }

    byte[] dataNoVersion = Arrays.copyOfRange(data.data, 1, data.data.length);
    byte[] bytes = convertBits(dataNoVersion, 5, 8, false);

    byte[] addressChars = Arrays.copyOfRange(bytes, 0, AccountAddressUtils.ACCOUNT_ADDRESS_LENGTH);

    Byte[] addressBytes = ArrayUtils.toObject(addressChars);
    AccountAddress accountAddress = new AccountAddress(Arrays.asList(addressBytes));

    return AccountAddressUtils.hex(accountAddress);
  }



  public static byte[] convertBits(final byte[] inputs, final int fromBits, final int toBits, final boolean pad)
      throws IllegalArgumentException {
    int acc = 0;
    int bits = 0;
    ByteArrayOutputStream out = new ByteArrayOutputStream(64);
    final int maxv = (1 << toBits) - 1;
    final int max_acc = (1 << (fromBits + toBits - 1)) - 1;
    for (int i = 0; i < inputs.length; i++) {
      int value = inputs[i] & 0xff;
      if ((value >>> fromBits) != 0) {
        throw new IllegalArgumentException(
            String.format("Input value '%X' exceeds '%d' bit" + " size", value,
                fromBits));
      }
      acc = ((acc << fromBits) | value) & max_acc;
      bits += fromBits;
      while (bits >= toBits) {
        bits -= toBits;
        out.write((acc >>> bits) & maxv);
      }
    }
    if (pad) {
      if (bits > 0) {
        out.write((acc << (toBits - bits)) & maxv);
      }
    } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
      throw new IllegalArgumentException("Could not convert bits, invalid padding");
    }
    return out.toByteArray();
  }

}
