package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChainAssets {
    String creator;
    Long id;
    BaseMeta base_meta;
    TypeMeta type_meta;
    Body body;


}
