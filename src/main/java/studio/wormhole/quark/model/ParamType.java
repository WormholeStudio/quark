package studio.wormhole.quark.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Splitter;
import com.novi.serde.Bytes;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.starcoin.utils.AccountAddressUtils;
import studio.wormhole.quark.helper.BcsSerializeHelper;
import studio.wormhole.quark.helper.move.MoveType;
import studio.wormhole.quark.helper.move.MoveTypeDeserialize;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamType {

  String name;
  @JSONField(deserializeUsing = MoveTypeDeserialize.class)
  ArgType type_tag;


  public Bytes bcsSerialize(String value) {

    MoveType moveType = getType_tag().getMoveType();
    if (moveType == MoveType.STRUCT || moveType == MoveType.SINGER) {
      throw new RuntimeException("Unsupported   :" + value + "," + moveType);
    }
    if (moveType == MoveType.VECTOR) {
      return serializeVector(getType_tag().argType, value);
    }
    return serializeBaseType(getType_tag().moveType, value);
  }

  private Bytes serializeVector(ArgType type, String value) {

    if (type.getMoveType() == MoveType.U8) {
      return BcsSerializeHelper.serializeVectorU8ToBytes(value);
    }
    List<String> list = Splitter.on(" ").trimResults().splitToList(value);

    List<Bytes> bytes = list.stream().map(s -> {
          if (type.getMoveType().isBaseType()) {
            return serializeBaseType(type.getMoveType(), s);
          }
          return serializeVector(type.getArgType(), s);
        }
    )
        .collect(Collectors.toList());
    return BcsSerializeHelper.serializeList(bytes);

  }

  private Bytes serializeBaseType(MoveType moveType, String value) {
    switch (moveType) {
      case ADDRESS:
        return BcsSerializeHelper.serializeAddressToBytes(AccountAddressUtils.create(value));
      case U8:
        return BcsSerializeHelper.serializeU8ToBytes(Byte.valueOf(value));
      case U64:
        return BcsSerializeHelper.serializeU64ToBytes(Long.valueOf(value));
      case U128:
        return BcsSerializeHelper.serializeU128ToBytes(new BigInteger(value));
      default:
        throw new RuntimeException("Unsupported   :" + value + "," + moveType);
    }
  }
}