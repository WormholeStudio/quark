package studio.wormhole.quark.helper;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class MoveFile {

  String name;
  List<String> depSet;
  String mvFilePath;
  String srcFilePath;
  String orderName;
  String type;
  List<String> typeList;
}
