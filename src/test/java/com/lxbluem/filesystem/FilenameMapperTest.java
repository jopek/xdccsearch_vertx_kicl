package com.lxbluem.filesystem;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FilenameMapperTest {

    private final FilenameMapper mapper = new FilenameMapper();

    @Test
    public void name_to_packname() {
        assertEquals("filename", mapper.createPackName("filename"));
        assertEquals("filename", mapper.createPackName("filename._x123x_"));
        assertEquals("filename", mapper.createPackName("filename._x123x_.part"));
        assertEquals("filename.txt", mapper.createPackName("filename.txt"));
        assertEquals("file.packname.mkv", mapper.createPackName("file.packname.mkv"));
        assertEquals("file.packname.mkv", mapper.createPackName("file.packname._x123x_.mkv"));
        assertEquals("file.packname.mkv", mapper.createPackName("file.packname._x123x_.mkv.part"));
        assertEquals("file.packname._x123x_.mkv", mapper.createPackName("file.packname._x123x_._x123x_.mkv"));
        assertEquals("file.packname._x123x_.part.mkv", mapper.createPackName("file.packname._x123x_.part._x123x_.mkv.part"));
    }

    @Test
    public void name_to_file_counter() throws Exception {
        assertEquals(0, mapper.createPackSuffix("filename"));
        assertEquals(123, mapper.createPackSuffix("filename._x123x_"));
        assertEquals(123, mapper.createPackSuffix("filename._x123x_.part"));
        assertEquals(123, mapper.createPackSuffix("filename._x567x_._x123x_.part"));
        assertEquals(123, mapper.createPackSuffix("filename._x567x_._x123x_"));
    }

    @Test
    public void packname_to_name() {
        assertEquals("filename._x0x_.part", mapper.getFsFilename("filename", 0));
        assertEquals("filename._x123123x_.part", mapper.getFsFilename("filename", 123123));
        assertEquals("filename._x200x_.txt.part", mapper.getFsFilename("filename.txt", 200));
    }

    @Test
    public void name() {
        List<Integer> ints = Arrays.asList();
        int suffix = ints
                .stream()
                .reduce(10, Math::max);

        assertEquals(10, suffix);
    }
}