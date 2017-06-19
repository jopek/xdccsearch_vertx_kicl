package com.lxbluem.filesystem;

import org.junit.Test;

import static org.junit.Assert.*;

public class FilenameMapperTest {

    private FilenameMapper mapper = new FilenameMapper();

    @Test
    public void name_to_packname() {
        assertEquals("filename", mapper.getPackName("filename"));
        assertEquals("filename", mapper.getPackName("filename._123_"));
        assertEquals("filename", mapper.getPackName("filename._123_.part"));
        assertEquals("filename.txt", mapper.getPackName("filename.txt"));
        assertEquals("file.packname.mkv", mapper.getPackName("file.packname.mkv"));
        assertEquals("file.packname.mkv", mapper.getPackName("file.packname._123_.mkv"));
        assertEquals("file.packname.mkv", mapper.getPackName("file.packname._123_.mkv.part"));
        assertEquals("file.packname._123_.mkv", mapper.getPackName("file.packname._123_._123_.mkv"));
        assertEquals("file.packname._123_.part.mkv", mapper.getPackName("file.packname._123_.part._123_.mkv.part"));
    }

    @Test
    public void name_to_file_counter() throws Exception {
        assertEquals(0, mapper.getPackSuffix("filename"));
        assertEquals(123, mapper.getPackSuffix("filename._123_"));
        assertEquals(123, mapper.getPackSuffix("filename._123_.part"));
        assertEquals(123, mapper.getPackSuffix("filename._567_._123_.part"));
        assertEquals(123, mapper.getPackSuffix("filename._567_._123_"));
    }

    @Test
    public void packname_to_name() {
        assertEquals("filename._123123_.part", mapper.getFsFilename("filename", 123123L));
        assertEquals("filename._200_.txt.part", mapper.getFsFilename("filename.txt", 200L));
    }


}