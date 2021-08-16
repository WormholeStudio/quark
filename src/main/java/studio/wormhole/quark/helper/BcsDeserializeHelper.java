package studio.wormhole.quark.helper;

import com.novi.bcs.BcsDeserializer;
import com.novi.serde.Bytes;
import lombok.SneakyThrows;

public class BcsDeserializeHelper {

    @SneakyThrows
    public static String toString(Bytes bytes) {
        BcsDeserializer s = new BcsDeserializer(bytes.content());
        return s.deserialize_str();
    }
}
