package com.lxbluem.filenameresolver.domain.ports.incoming;

import com.lxbluem.filenameresolver.adapters.InMemoryEntityStorage;
import com.lxbluem.filenameresolver.domain.interactors.FilenameMapper;
import com.lxbluem.filenameresolver.domain.interactors.ResolvePackNameImpl;
import com.lxbluem.filenameresolver.domain.model.FileEntity;
import com.lxbluem.filenameresolver.domain.ports.incoming.ResolvePackName.Response;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileEntityStorage;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystemBlocking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ResolvePackNameTest {

    private ResolvePackName resolvePackName;
    private FileSystemBlocking fileSystem;
    private FileEntityStorage fileEntityStorage;

    @BeforeEach
    void setUp() {
        fileSystem = Mockito.mock(FileSystemBlocking.class);
        fileEntityStorage = new InMemoryEntityStorage();
        FilenameMapper filenameMapper = new FilenameMapper();
        resolvePackName = new ResolvePackNameImpl(filenameMapper, fileSystem, fileEntityStorage, "downloads");
    }

    @Test
    void filename_not_present() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Collections.emptyList());

        Response expected = new Response("downloads/someFile._x0x_.part", 0, false);
        resolvePackName.execute("someFile", 10000L)
                .test()
                .assertValue(expected)
                .unsubscribe();
    }

    @Test
    void filename_not_present_amongst_others() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList(
                        "otherFile._x0x_.part",
                        "anotherFile._x2x_.part",
                        "yetAnotherFile._x0x_"
                        )
                );

        Response expected = new Response("downloads/someFile._x0x_.part", 0, false);
        resolvePackName.execute("someFile", 10000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertFalse(fileEntityList.isEmpty());
        assertEquals(new FileEntity("someFile", 10000L, "someFile._x0x_.part", true), fileEntityList.get(0));
    }

    @Test
    void filename_present__position_eof__complete() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList("someFile._x1x_"));
        when(fileSystem.fileSize("someFile._x1x_"))
                .thenReturn(10_000L);

        Response expected = new Response("downloads/someFile._x1x_", 10000L, true);

        resolvePackName.execute("someFile", 10000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertTrue(fileEntityList.isEmpty());
    }

    @Test
    void filename_present__position_eof__complete__multiple() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList(
                        "someFile._x0x_",
                        "someFile._x1x_",
                        "someFile._x2x_",
                        "someFile._x3x_",
                        "someFile._x4x_.part"
                ));
        when(fileSystem.fileSize("someFile._x0x_")).thenReturn(8_000L);
        when(fileSystem.fileSize("someFile._x1x_")).thenReturn(10_000L);
        when(fileSystem.fileSize("someFile._x2x_")).thenReturn(8_000L);
        when(fileSystem.fileSize("someFile._x3x_")).thenReturn(10_000L);
        when(fileSystem.fileSize("someFile._x4x_")).thenReturn(7_000L);

        Response expected = new Response("downloads/someFile._x3x_", 10_000L, true);

        resolvePackName.execute("someFile", 10_000L)
                .test()
                .assertValue(expected)
                .unsubscribe();
    }

    @Test
    void filename_present__position_eof__complete__multiple_2() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList(
                        "/canonical/path/to/downloads/test3-1m._x0x_.bin",
                        "/canonical/path/to/downloads/test3-1m._x0x_.bin.part",
                        "/canonical/path/to/downloads/test3-1m._x1x_.bin",
                        "/canonical/path/to/downloads/test3-1m._x2x_.bin.part",
                        "/canonical/path/to/downloads/test3-1m._x3x_.bin.part",
                        "/canonical/path/to/downloads/test3-1m._x4x_.bin",
                        "/canonical/path/to/downloads/test3-1m._x5x_.bin",
                        "/canonical/path/to/downloads/test3-1m.bin"
                ));

        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m._x0x_.bin")).thenReturn(137_240L);
        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m._x0x_.bin.part")).thenReturn(67_160L);
        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m._x1x_.bin")).thenReturn(1_048_576L);
        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m._x2x_.bin.part")).thenReturn(39_420L);
        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m._x3x_.bin.part")).thenReturn(51_100L);
        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m._x4x_.bin")).thenReturn(1_048_576L);
        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m._x5x_.bin")).thenReturn(55_480L);
        when(fileSystem.fileSize("/canonical/path/to/downloads/test3-1m.bin")).thenReturn(1_048_576L);

        Response expected = new Response("downloads/test3-1m.bin", 1_048_576L, true);

        resolvePackName.execute("test3-1m.bin", 1_048_576L)
                .test()
                .assertValue(expected)
                .unsubscribe();
    }

    @Test
    void filename_present__position_eof__complete__multiple__not_matching_size() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList(
                        "someFile._x0x_",
                        "someFile._x1x_",
                        "someFile._x2x_"
                ));
        when(fileSystem.fileSize("someFile._x0x_")).thenReturn(9_000L);
        when(fileSystem.fileSize("someFile._x1x_")).thenReturn(8_000L);
        when(fileSystem.fileSize("someFile._x2x_")).thenReturn(7_000L);

        Response expected = new Response("downloads/someFile._x3x_.part", 0, false);

        resolvePackName.execute("someFile", 10_000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertFalse(fileEntityList.isEmpty());
    }

    @Test
    void filename_present__position_eof__complete__multiple__not_matching_size_and_partial() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList(
                        "someFile._x0x_",
                        "someFile._x1x_",
                        "someFile._x2x_",
                        "someFile._x3x_.part"
                ));
        when(fileSystem.fileSize("someFile._x0x_")).thenReturn(9_000L);
        when(fileSystem.fileSize("someFile._x1x_")).thenReturn(8_000L);
        when(fileSystem.fileSize("someFile._x2x_")).thenReturn(7_000L);
        when(fileSystem.fileSize("someFile._x3x_.part")).thenReturn(4_000L);
        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x3x_.part", false));

        Response expected = new Response("downloads/someFile._x3x_.part", 4_000L, false);

        resolvePackName.execute("someFile", 10_000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertFalse(fileEntityList.isEmpty());
    }

    @Test
    void filename_present__position_in_file__incomplete__not_in_use() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList("/path/downloads/someFile._x0x_.part"));
        when(fileSystem.fileSize("/path/downloads/someFile._x0x_.part"))
                .thenReturn(5_000L);
        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x0x_.part", false));

        Response expected = new Response("downloads/someFile._x0x_.part", 5_000L, false);
        resolvePackName.execute("someFile", 10000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertEquals(1, fileEntityList.size());
        fileEntityStorage.findByFileName("someFile._x0x_.part")
                .ifPresent(f -> assertTrue(f.isInUse()));
    }

    @Test
    void filename_present__position_in_file__incomplete__not_in_use__multiple() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList(
                        "someFile._x0x_.part",
                        "someFile._x1x_.part"
                ));
        when(fileSystem.fileSize("someFile._x0x_.part")).thenReturn(5_000L);
        when(fileSystem.fileSize("someFile._x1x_.part")).thenReturn(8_000L);

        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x0x_.part", false));
        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x1x_.part", false));

        Response expected = new Response("downloads/someFile._x1x_.part", 8_000L, false);
        resolvePackName.execute("someFile", 10000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertEquals(2, fileEntityList.size());
        fileEntityStorage.findByFileName("someFile._x0x_.part")
                .ifPresent(f -> assertFalse(f.isInUse()));
        fileEntityStorage.findByFileName("someFile._x1x_.part")
                .ifPresent(f -> assertTrue(f.isInUse()));
    }

    @Test
    void filename_present__position_in_file__incomplete__in_use() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList("someFile._x0x_.part"));
        when(fileSystem.fileSize("someFile._x0x_.part"))
                .thenReturn(5_000L);
        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x0x_.part", true));

        Response expected = new Response("downloads/someFile._x1x_.part", 0, false);
        resolvePackName.execute("someFile", 10000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertEquals(2, fileEntityList.size());
        fileEntityStorage.findByFileName("someFile._x0x_.part")
                .ifPresent(f -> assertTrue(f.isInUse()));
        fileEntityStorage.findByFileName("someFile._x1x_.part")
                .ifPresent(f -> assertTrue(f.isInUse()));
    }

    @Test
    void filename_present__position_in_file__incomplete__in_use__multiple() {
        when(fileSystem.readDir("downloads"))
                .thenReturn(Arrays.asList(
                        "someFile._x0x_.part",
                        "someFile._x1x_.part",
                        "someFile._x2x_.part",
                        "someFile._x3x_.part",
                        "someOtherFile._x0x_.part",
                        "anotherFile._x0x_"
                ));
        when(fileSystem.fileSize("someFile._x0x_.part")).thenReturn(5_000L);
        when(fileSystem.fileSize("someFile._x1x_.part")).thenReturn(7_000L);
        when(fileSystem.fileSize("someFile._x2x_.part")).thenReturn(8_000L);
        when(fileSystem.fileSize("someFile._x3x_.part")).thenReturn(9_000L);
        when(fileSystem.fileSize("someOtherFile._x0x_.part")).thenReturn(10_000L);
        when(fileSystem.fileSize("anotherFile._x0x_")).thenReturn(10_000L);

        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x0x_.part", true));
        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x1x_.part", false));
        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x2x_.part", true));
        fileEntityStorage.save(new FileEntity("someFile", 10_000L, "someFile._x3x_.part", false));
        fileEntityStorage.save(new FileEntity("someOtherFile", 10_000L, "someOtherFile._x0x_.part", false));

        Response expected = new Response("downloads/someFile._x3x_.part", 9_000L, false);
        resolvePackName.execute("someFile", 10_000L)
                .test()
                .assertValue(expected)
                .unsubscribe();

        List<FileEntity> fileEntityList = fileEntityStorage.findByNameAndSize("someFile", 10000L);
        assertEquals(4, fileEntityList.size());
        fileEntityStorage.findByFileName("someFile._x0x_.part")
                .ifPresent(f -> assertTrue(f.isInUse()));
        fileEntityStorage.findByFileName("someFile._x1x_.part")
                .ifPresent(f -> assertFalse(f.isInUse()));
        fileEntityStorage.findByFileName("someFile._x2x_.part")
                .ifPresent(f -> assertTrue(f.isInUse()));
        fileEntityStorage.findByFileName("someFile._x3x_.part")
                .ifPresent(f -> assertTrue(f.isInUse()));
        fileEntityStorage.findByFileName("someOtherFile._x0x_.part")
                .ifPresent(f -> assertFalse(f.isInUse()));
    }

}
