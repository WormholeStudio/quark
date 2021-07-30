package studio.wormhole.quark.helper;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

public class MovePackageUtil {


  public static List<MoveFile> prePackageFile(String src) {
    Iterable<File> fileIterable = Files
        .fileTraverser()
        .breadthFirst(new File(src));
    Map<String, List<MoveFile>> moveFilesGroup = Streams.stream(fileIterable)
        .filter(s -> s.isFile() && !s.isHidden())
        .filter(s -> !s.getAbsolutePath().contains("/storage/0x00000000000000000000000000000001/"))
        .filter(s -> StringUtils.endsWithAny(s.getName(), "move", "mv"))
        .map(f -> {
          Path path = f.toPath();
          Path path1 = Paths.get(src).resolve(path.getParent());
          List<String> type = Splitter.on("/").omitEmptyStrings()
              .splitToList(path1.toString().replace(src, ""));
          return MoveFile.builder().name(f.getName())
              .srcFilePath(f.getAbsolutePath())
              .type(type.get(0))
              .typeList(type)
              .depSet(depSet(f)).build();
        })
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(MoveFile::getType));

    List<MoveFile> srcFiles = sort(moveFilesGroup.get("src"));
    Map<String, String> mvFilesMap = moveFilesGroup.get("storage").stream()
        .map(s -> Maps.immutableEntry(s.getName(), s.getSrcFilePath()))
        .collect(Collectors.toMap(s -> s.getKey(), s -> s.getValue()));
    List<MoveFile> files = srcFiles.stream().map(s -> {
      String mvName = s.getName().replace(".move", ".mv");
      return s.toBuilder().mvFilePath(mvFilesMap.get(mvName)).build();
    }).sorted((o1, o2) -> StringUtils.compare(o1.getOrderName(), o2.getOrderName()))
        .collect(Collectors.toList());
    return files;
  }


  private static void blackMagic(List<MoveFile> graph, String v, Map<String, Boolean> visited,
      List<String> order) {
    visited.replace(v, true);
    List<String> depSet = graph.stream().filter(s -> s.getName().equals(v)).findFirst()
        .map(MoveFile::getDepSet).orElse(Lists.newArrayList());
    if (depSet == null) {
      depSet = Lists.newArrayList();
    }
    for (String neighborId : depSet) {

      if (!visited.get(neighborId)) {
        blackMagic(graph, neighborId, visited, order);
      }
    }
    order.add(v);
  }

  private static List<MoveFile> sort(List<MoveFile> graph) {
    List<String> order = Lists.newArrayList();
    Map<String, Boolean> visited = Maps.newHashMap();
    for (MoveFile tmp : graph) {
      visited.put(tmp.getName(), false);
    }
    for (MoveFile tmp : graph) {
      if (!visited.get(tmp.getName())) {
        blackMagic(graph, tmp.getName(), visited, order);
      }
    }
    return graph.stream().map(f -> {
      int index = order.indexOf(f.getName()) + 1;
      String pre = String.format("%04d", Integer.valueOf(index));
      String name = f.getName().replace("move", "mv");
      return f.toBuilder().orderName(pre + "_" + name).build();
    }).collect(Collectors.toList());
  }

  private static Pattern p = Pattern.compile("use\\s+(0x[a-f0-9A-F]+?)::([\\x00-\\xff]+?);");

  @SneakyThrows
  private static List<String> depSet(File s) {
    return Files.readLines(s, StandardCharsets.UTF_8).stream().map(line -> {
      Matcher matcher = p.matcher(line);
      if (matcher.find()) {
        if (StringUtils.equalsIgnoreCase(matcher.group(1), "0x1")) {
          return null;
        }
        return matcher.group(2) + ".move";
      }
      return null;
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
