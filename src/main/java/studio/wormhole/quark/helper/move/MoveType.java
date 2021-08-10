package studio.wormhole.quark.helper.move;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MoveType {

  SINGER("Signer"),
  ADDRESS("Address"),
  U8("u8"),
  U64("u64"),
  U128("u128"),
  VECTOR("Vector"),
  STRUCT("Struct"),

  ;
  private String name;

  public static MoveType fromString(String string) {
    return Arrays.stream(MoveType.values()).filter(s -> s.getName().equalsIgnoreCase(string))
        .findAny()
        .orElseThrow(() -> new RuntimeException("Unsupported move type:" + string));
  }

  public   boolean isBaseType( ) {

    if (this == SINGER || this == STRUCT || this == VECTOR) {
      return false;
    }
    return true;
  }
}
