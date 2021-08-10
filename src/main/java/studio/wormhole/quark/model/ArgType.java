package studio.wormhole.quark.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import studio.wormhole.quark.helper.move.MoveType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgType {
  MoveType moveType;
  ArgType argType;
}
