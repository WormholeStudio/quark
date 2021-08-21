package studio.wormhole.quark.command.alma.airdrop;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.collect.Streams.FunctionWithIndex;
import com.google.common.primitives.Bytes;
import lombok.SneakyThrows;
import org.starcoin.utils.AccountAddressUtils;
import org.starcoin.utils.BcsSerializeHelper;
import org.starcoin.utils.Hex;
import studio.wormhole.quark.command.alma.airdrop.merkletree.MerkleHash;
import studio.wormhole.quark.command.alma.airdrop.merkletree.MerkleProofHash;
import studio.wormhole.quark.command.alma.airdrop.merkletree.MerkleTree;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class MerkleTreeHelper {


  public static ApiMerkleTree merkleTree(long airdropId, List<CSVRecord> recordList) {

    MerkleTree merkleTree = new MerkleTree();

    if (recordList.size() % 2 != 0) {
      CSVRecord csvRecord = CSVRecord.builder()
          .address("0x00000000000000000000000000000000")
          .amount(new BigInteger("0"))
          .build();
      recordList.add(csvRecord);
    }
    Map<String, CSVRecord> maps = Streams
        .mapWithIndex(recordList.stream(), new FunctionWithIndex<CSVRecord, Entry>() {
          @SneakyThrows
          @Override
          public Entry apply(CSVRecord csvRecord, long idx) {
            byte[] indexBytes = BcsSerializeHelper.serializeU64(Long.valueOf(idx));
            byte[] airdropIdBytes = BcsSerializeHelper.serializeU64(airdropId);
            byte[] amountBytes = BcsSerializeHelper.serializeU128(csvRecord.getAmount());
            byte[] addressBytes = AccountAddressUtils.create(csvRecord.getAddress()).bcsSerialize();
            MerkleHash hash = MerkleHash
                .create(Bytes.concat(indexBytes, airdropIdBytes, addressBytes, amountBytes));
            merkleTree.appendLeaf(hash);
            return Maps.immutableEntry(Hex.encode(hash.getValue()),
                csvRecord.toBuilder().index(idx).build());
          }
        }).collect(Collectors.toMap(k -> (String) k.getKey(), v -> (CSVRecord) v.getValue()));

    MerkleHash rootHash = merkleTree.buildTree();

    List<ApiMerkleProof> apiMerkleProofs = merkleTree.getLeaves().stream().map(leaf -> {
      String hash = Hex.encode(leaf.getHash().getValue());
      CSVRecord record = maps.get(hash);

      if (record.getAddress().equals("0x00000000000000000000000000000000")) {
        return null;
      }
      List<MerkleProofHash> proofHashes = merkleTree.auditProof(leaf.getHash());
      List<String> proofList = proofHashes.stream()
          .map(s -> Hex.encode(s.getHash().getValue())).collect(Collectors.toList());
      return ApiMerkleProof.builder()
          .address(record.getAddress())
          .amount(record.getAmount())
          .index(record.getIndex())
          .proof(proofList)
          .build();

    }).filter(Objects::nonNull).collect(Collectors.toList());
    return ApiMerkleTree.builder()
        .proofs(apiMerkleProofs)
        .root(Hex.encode(rootHash.getValue())).build();


  }


}
