package studio.wormhole.quark.command.alma.airdrop.merkletree;

import com.google.common.primitives.Bytes;
import lombok.Data;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.Arrays;

import java.nio.charset.StandardCharsets;

@Data
public class MerkleHash {

  /**
   * Hash value as byte array.
   */
  private byte[] value;

  public MerkleHash() {
  }

  /**
   * Create a MerkleHash from an array of bytes.
   *
   * @param buffer of bytes
   * @return a MerkleHash
   */
  public static MerkleHash create(byte[] buffer) {
    MerkleHash hash = new MerkleHash();
    hash.computeHash(buffer);
    return hash;
  }

  /**
   * Create a MerkleHash from a string. The string needs first to be transformed in a UTF8 sequence
   * of bytes. Used for leaf hashes.
   *
   * @param buffer string
   * @return a MerkleHash
   */
  public static MerkleHash create(String buffer) {
    return create(buffer.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Create a MerkleHash from two MerkleHashes by concatenation of the byte arrays. Used for
   * internal nodes.
   *
   * @param left  subtree hash
   * @param right subtree hash
   * @return a MerkleHash
   */
  public static MerkleHash create(MerkleHash left, MerkleHash right) {
    return create(concatenate(left.getValue(), right.getValue()));
  }


  /**
   * Compute SHA256 hash of a byte array.
   *
   * @param buffer of bytes
   */
  private void computeHash(byte[] buffer) {
    SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest256();
    this.value = digestSHA3.digest(buffer);
  }

  /**
   * Concatenate two array of bytes.
   *
   * @param a is the first array
   * @param b is the second array
   * @return a byte array
   */
  public static byte[] concatenate(byte[] a, byte[] b) {
    if (Arrays.compareUnsigned(a, b) == 1) {
      return Bytes.concat(b, a);
    }
    return Bytes.concat(a, b);
  }
}
