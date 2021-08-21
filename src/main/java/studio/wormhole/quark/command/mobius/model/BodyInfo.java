package studio.wormhole.quark.command.mobius.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BodyInfo {
    long last_update_at;
    List<Equivalents> collateral;
    List<Equivalents> debt;
}
