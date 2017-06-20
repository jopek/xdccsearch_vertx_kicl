package com.lxbluem.filesystem;

import org.junit.Test;

import static org.junit.Assert.*;

public class FilenameMapperTest {

    private FilenameMapper mapper = new FilenameMapper();

    @Test
    public void name_to_packname() {
        assertEquals("filename", mapper.createPackName("filename"));
        assertEquals("filename", mapper.createPackName("filename._123_"));
        assertEquals("filename", mapper.createPackName("filename._123_.part"));
        assertEquals("filename.txt", mapper.createPackName("filename.txt"));
        assertEquals("file.packname.mkv", mapper.createPackName("file.packname.mkv"));
        assertEquals("file.packname.mkv", mapper.createPackName("file.packname._123_.mkv"));
        assertEquals("file.packname.mkv", mapper.createPackName("file.packname._123_.mkv.part"));
        assertEquals("file.packname._123_.mkv", mapper.createPackName("file.packname._123_._123_.mkv"));
        assertEquals("file.packname._123_.part.mkv", mapper.createPackName("file.packname._123_.part._123_.mkv.part"));
    }

    @Test
    public void name_to_file_counter() throws Exception {
        assertEquals(0, mapper.createPackSuffix("filename"));
        assertEquals(123, mapper.createPackSuffix("filename._123_"));
        assertEquals(123, mapper.createPackSuffix("filename._123_.part"));
        assertEquals(123, mapper.createPackSuffix("filename._567_._123_.part"));
        assertEquals(123, mapper.createPackSuffix("filename._567_._123_"));
    }

    @Test
    public void packname_to_name() {
        assertEquals("filename._0_.part", mapper.getFsFilename("filename", 0));
        assertEquals("filename._123123_.part", mapper.getFsFilename("filename", 123123));
        assertEquals("filename._200_.txt.part", mapper.getFsFilename("filename.txt", 200));
    }


}