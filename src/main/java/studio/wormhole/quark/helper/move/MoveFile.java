package studio.wormhole.quark.helper.move;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class MoveFile {

  String name;
  List<String> depSet;
  String mvFilePath;
  String srcFilePath;
  String type;
  List<String> typeList;

  String moduleName;
  String orderName;


}
