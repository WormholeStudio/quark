package studio.wormhole.quark.helper.move;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MovePackageUtil {

    private static Pattern p = Pattern.compile("use\\s+(0x[a-f0-9A-F]+?)::([\\x00-\\xff]+?);");
    private static Pattern m = Pattern.compile("\\s*module\\s+([\"\\w- ]*)\\{");

    public static void publish(String src, String starcoinRpc) {
        Process process;
        try {
            String cmd = "move clean &&move check  && move publish";
            if (StringUtils.isNotEmpty(starcoinRpc)) {
                cmd = "move clean &&move check  --mode starcoin --starcoin-rpc " + starcoinRpc + "  && move publish   ";
            }
            ProcessBuilder processBuilder = new ProcessBuilder(new String[]{"/bin/sh", "-c", cmd});
            processBuilder.directory(new File(src));
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static void replaceAddress(String src, String oldAddress, String newAddress) {

        Map<String, List<MoveFile>> moveFilesGroup = buildMoveFilesGroup(src);
        List<MoveFile> srcFiles = sort(moveFilesGroup.get("src"));

        srcFiles.parallelStream().forEach(s -> {
            File f = new File(s.getSrcFilePath());
            try {
                String data = FileUtils.readFileToString(f, Charset.defaultCharset());
                FileUtils.writeStringToFile(f, data.replaceAll(oldAddress, newAddress), Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


    }

    public static List<MoveFile> prePackageFile(String src) {


        Map<String, List<MoveFile>> moveFilesGroup = buildMoveFilesGroup(src);


        List<MoveFile> srcFiles = sort(moveFilesGroup.get("src"));
        Map<String, String> mvFilesMap = moveFilesGroup.get("storage").stream()
                .map(s -> Maps.immutableEntry(s.getName(), s.getSrcFilePath()))
                .collect(Collectors.toMap(s -> s.getKey(), s -> s.getValue()));

        List<MoveFile> files = srcFiles.stream().map(s -> {
                    String mvName = s.getName().replace(".move", ".mv");
                    return s.toBuilder().mvFilePath(mvFilesMap.get(mvName)).build();
                })
                .filter(s->!s.getSrcFilePath().contains("src/dep/"))
                .sorted((o1, o2) -> StringUtils.compare(o1.getOrderName(), o2.getOrderName()))
                .collect(Collectors.toList());
        return files;
    }

    private static Map<String, List<MoveFile>> buildMoveFilesGroup(String src) {
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

                    if ("src".equals(type.get(0)) && f.getName().endsWith(".move")) {
                        try {
                            CharStreams
                                    .readLines(new FileReader(f), new LineProcessor<List<MoveFile>>() {

                                        List<MoveFile> rst = Lists.newArrayList();

                                        boolean firstModule = false;
                                        boolean moduleStart = false;
                                        List<String> moduleLines;

                                        MoveFile moveFile;

                                        @Override
                                        public boolean processLine(String line) throws IOException {
                                            Matcher matcher = m.matcher(line);
                                            moduleStart = matcher.find();
                                            if (moduleStart) {
                                                if (!CollectionUtils.isEmpty(moduleLines)) {
                                                    rst.add(MoveFile.builder().name(rst.size() + 1 + "").build());
                                                }
                                                moveFile = new MoveFile();
                                                moveFile.setModuleName(matcher.group(1));
                                                moduleLines = Lists.newArrayList();
                                            }
                                            if (moduleLines != null) {
                                                moduleLines.add(line);
                                            }

                                            return true;
                                        }

                                        @Override
                                        public List<MoveFile> getResult() {
                                            if (moveFile != null) {
                                                rst.add(MoveFile.builder().name(rst.size() + 1 + "").build());
                                            }
                                            return rst;
                                        }
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return MoveFile.builder().name(f.getName())
                            .srcFilePath(f.getAbsolutePath())
                            .type(type.get(0))
                            .typeList(type)
                            .depSet(depSet(f)).build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(MoveFile::getType));
        return moveFilesGroup;
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
            if (!visited.containsKey(neighborId)) {
                continue;
            }
            if (!visited.get(neighborId)) {
                blackMagic(graph, neighborId, visited, order);
            }
        }
        order.add(v);
    }

    private static List<MoveFile> sort(List<MoveFile> graph) {
        if (graph.size() == 1) {
            return graph;
        }
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
            String name = f.getName().trim().replace("move", "mv");
            return f.toBuilder().orderName(pre + "_" + name).build();
        }).collect(Collectors.toList());
    }


    @SneakyThrows
    private static List<String> depSet(File s) {
        return Files.readLines(s, StandardCharsets.UTF_8).stream().map(line -> {
            Matcher matcher = p.matcher(line);
            if (matcher.find()) {
                if (StringUtils.equalsIgnoreCase(matcher.group(1), "0x1")) {
                    return null;
                }
                String deps = matcher.group(2).trim();
                if (deps.contains("::")) {
                    deps = deps.split("::")[0];
                }
                return deps + ".move";
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
