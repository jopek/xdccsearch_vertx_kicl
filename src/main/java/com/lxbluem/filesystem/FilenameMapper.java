package com.lxbluem.filesystem;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

public class FilenameMapper {

    private static final String PART = "part";
    private static final String PATTERN = "_x\\d+x_";

    public String createPackName(String name) {
        List<String> split = getNameElements(name);

        if (split.size() == 1)
            return name;

        Pattern p = Pattern.compile(PATTERN);

        MutableLong patternCount = new MutableLong(
                split.stream()
                        .filter(s -> p.matcher(s).find())
                        .count()
        );

        MutableLong partCount = new MutableLong(
                split.stream()
                        .filter(s -> s.equalsIgnoreCase(PART))
                        .count()
        );

        return split.stream()
                .filter(s -> noPatternMatch(s, p, patternCount))
                .filter(s -> noStringMatch(s, PART, partCount))
                .collect(joining("."));
    }

    private List<String> getNameElements(String name) {
        return new ArrayList<>(Arrays.asList(name.split("\\.")));
    }

    private boolean noPatternMatch(String s, Pattern p, MutableLong mutableLong) {
        if (p.matcher(s).find()) {
            mutableLong.decrement();
            return mutableLong.getValue() > 0;
        }
        return true;
    }

    private boolean noStringMatch(String s, String p, MutableLong mutableLong) {
        if (p.equalsIgnoreCase(s)) {
            mutableLong.decrement();
            return mutableLong.getValue() > 0;
        }
        return true;
    }

    public String getFsFilename(String packFilename, int suffix) {
        return getFsFilename(packFilename, suffix, true);
    }
    public String getFsFilename(String packFilename, int suffix, boolean addPartSuffix) {
        List<String> nameElements = getNameElements(packFilename);
        int size = nameElements.size();

        if (size == 1) {
            nameElements.add(String.format("_x%dx_", suffix));
        } else {
            String extension = nameElements.get(size - 1);
            nameElements.remove(size - 1);
            nameElements.add(String.format("_x%dx_", suffix));
            nameElements.add(extension);
        }

        if (addPartSuffix)
            nameElements.add(PART);

        return StringUtils.join(nameElements, '.');
    }

    public int createPackSuffix(String filename) {
        List<String> split = getNameElements(filename);

        if (split.size() == 1)
            return 0;

        Pattern p = Pattern.compile(PATTERN);

        MutableLong patternCount = new MutableLong(
                split.stream()
                        .filter(s -> p.matcher(s).find())
                        .count()
        );

        return split.stream()
                .filter(s -> !noPatternMatch(s, p, patternCount))
                .map(s -> s.replaceAll("[^0-9]", ""))
                .map(Integer::parseInt)
                .findFirst()
                .orElse(0);
    }

    public int createHash(String safeFilename, long filesize) {
        String toBeHashed = safeFilename + filesize;
        int hashCode = toBeHashed.hashCode();
        return Math.abs(hashCode) % 10;
    }
}
