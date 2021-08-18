package studio.wormhole.quark.helper;

import com.google.common.collect.Lists;
import com.novi.bcs.BcsSerializer;
import com.novi.serde.Bytes;
import lombok.SneakyThrows;
import org.starcoin.types.AccountAddress;
import org.starcoin.utils.Hex;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

public class BcsSerializeHelper {

    @SneakyThrows
    public static Bytes serializeU8ToBytes(Byte v) {
        BcsSerializer s = new BcsSerializer();
        s.serialize_u8(v);
        return Bytes.valueOf(s.get_bytes());
    }

    @SneakyThrows
    public static Bytes serializeU128ToBytes(BigInteger v) {
        BcsSerializer s = new BcsSerializer();
        s.serialize_u128(v);
        return Bytes.valueOf(s.get_bytes());
    }


    @SneakyThrows
    public static Bytes serializeAddressToBytes(AccountAddress address) {
        return Bytes.valueOf(address.bcsSerialize());
    }

    @SneakyThrows
    public static Bytes serializeU64ToBytes(Long v) {
        BcsSerializer s = new BcsSerializer();
        s.serialize_u64(v);
        return Bytes.valueOf(s.get_bytes());
    }

    @SneakyThrows
    public static Bytes serializeStrToBytes(String str) {
        BcsSerializer s = new BcsSerializer();
        s.serialize_str(str);
        return Bytes.valueOf(s.get_bytes());

    }


    @SneakyThrows
    public static Bytes serializeListToBytes(List<String> list) {
        List<Bytes> bytesList = list.stream().map(s -> {
                    if (s.startsWith("0x")) {
                        return new Bytes(Hex.decode(s));
                    }
                    return serializeStrToBytes(s);
                })
                .collect(Collectors.toList());
        BcsSerializer s = new BcsSerializer();
        s.serialize_len(bytesList.size());
        for (Bytes item : bytesList) {
            s.serialize_bytes(item);
        }
        byte[] bytes = s.get_bytes();
        return Bytes.valueOf(bytes);
    }


    @SneakyThrows
    public static byte[] serializeU64(Long v) {
        long value = v.longValue();
        com.novi.serde.Serializer serializer = new BcsSerializer();
        serializer.increase_container_depth();
        serializer.serialize_u64(value);
        serializer.decrease_container_depth();
        return serializer.get_bytes();
    }

    @SneakyThrows
    public static byte[] serializeU128(BigInteger v) {
        com.novi.serde.Serializer serializer = new BcsSerializer();
        serializer.increase_container_depth();
        serializer.serialize_u128(v);
        serializer.decrease_container_depth();
        return serializer.get_bytes();
    }

    @SneakyThrows
    public static byte[] serializeString(String v) {
        com.novi.serde.Serializer serializer = new BcsSerializer();
        serializer.increase_container_depth();
        serializer.serialize_str(v);
        serializer.decrease_container_depth();
        return serializer.get_bytes();
    }

    @SneakyThrows
    public static byte[] serializeU8(Byte v) {
        com.novi.serde.Serializer serializer = new BcsSerializer();
        serializer.increase_container_depth();
        serializer.serialize_u8(v);
        serializer.decrease_container_depth();
        return serializer.get_bytes();
    }

    @SneakyThrows
    public static byte[] serializeVectorU8(List<Byte> value) {
        com.novi.serde.Serializer serializer = new BcsSerializer();

        serializer.serialize_len(value.size());
        for (@com.novi.serde.Unsigned Byte item : value) {
            serializer.serialize_u8(item);
        }
        return serializer.get_bytes();
    }

    @SneakyThrows
    public static Bytes serializeList(List<Bytes> bytesList) {
        BcsSerializer s = new BcsSerializer();
        s.serialize_len(bytesList.size());
        for (Bytes item : bytesList) {
            s.serialize_bytes(item);
        }
        byte[] bytes = s.get_bytes();
        return Bytes.valueOf(bytes);
    }

    @SneakyThrows
    public static Bytes serializeHexStringToVectorU8(String hexString) {
        byte[] bytes = Hex.decode(hexString);

        List<Byte> list = Lists.newArrayList();
        for (byte b : bytes) {
            list.add(b);
        }
        return new Bytes(BcsSerializeHelper.serializeVectorU8(list));
    }


    @SneakyThrows
    public static Bytes serializeVectorAddress(List<AccountAddress> list) {
        BcsSerializer s = new BcsSerializer();
        s.serialize_len(list.size());
        for (AccountAddress item : list) {
            item.serialize(s);
        }
        byte[] bytes = s.get_bytes();
        return Bytes.valueOf(bytes);
    }

    @SneakyThrows
    public static Bytes serializeVectorU128(List<BigInteger> list) {
        BcsSerializer s = new BcsSerializer();
        s.serialize_len(list.size());
        for (BigInteger item : list) {
            s.serialize_u128(item);
        }
        byte[] bytes = s.get_bytes();
        return Bytes.valueOf(bytes);
    }

}
