package studio.wormhole.quark.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Txn {
  String txn;
  String message;
}
