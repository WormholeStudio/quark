package studio.wormhole.quark.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Splitter;
import com.novi.serde.Bytes;
import lombok.*;
import org.starcoin.utils.AccountAddressUtils;
import org.starcoin.utils.Hex;
import studio.wormhole.quark.helper.BcsSerializeHelper;
import studio.wormhole.quark.helper.move.MoveType;
import studio.wormhole.quark.helper.move.MoveTypeDeserialize;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

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
            return serializeVector(getType_tag().argType, value, null);
        }
        return serializeBaseType(getType_tag().moveType, value);
    }

    @SneakyThrows
    private Bytes serializeVector(ArgType type, String value, ArgType parentType) {

        if (type.getMoveType() == MoveType.U8) {
            if (value.startsWith("0x")) {
                if (parentType != null && parentType.getMoveType() == MoveType.VECTOR) {
                    return new Bytes(Hex.decode(value));
                }
                return BcsSerializeHelper.serializeHexStringToVectorU8(value);
            }
            return BcsSerializeHelper.serializeStrToBytes(value);
        }

        List<String> list = Splitter.on(" ").trimResults().splitToList(value);

        List<Bytes> bytes = list
                .stream()
                .map(s -> {
                            if (type.getMoveType().isBaseType()) {
                                return serializeBaseType(type.getMoveType(), s);
                            }
                            return serializeVector(type.getArgType(), s, type);
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