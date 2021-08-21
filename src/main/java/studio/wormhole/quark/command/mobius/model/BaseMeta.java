package studio.wormhole.quark.command.mobius.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseMeta {
    @JSONField(deserializeUsing = HexDeserializer.class)
    String images;
    @JSONField(deserializeUsing = HexDeserializer.class)
    String image_data;
    @JSONField(deserializeUsing = HexDeserializer.class)
    String name;
    @JSONField(deserializeUsing = HexDeserializer.class)
    String description;
}
