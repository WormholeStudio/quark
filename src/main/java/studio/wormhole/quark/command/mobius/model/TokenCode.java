package studio.wormhole.quark.command.mobius.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenCode {

    @JSONField(deserializeUsing = HexDeserializer.class)
    String name;
    @JSONField(deserializeUsing = HexDeserializer.class)
    String module_name;
    String addr;


    public String toAddress() {

        return addr + "::" + module_name + "::" + name;
    }
}
