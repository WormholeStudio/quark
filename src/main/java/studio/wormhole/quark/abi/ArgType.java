package studio.wormhole.quark.abi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArgType {
  MoveType moveType;
  ArgType argType;
}
