package com.lxbluem.filenameresolver.domain.interactors;

import com.lxbluem.filenameresolver.adapters.InMemoryEntityStorage;
import com.lxbluem.filenameresolver.domain.model.FileEntity;
import com.lxbluem.filenameresolver.domain.ports.incoming.SyncStorageFromFs;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileEntityStorage;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystemBlocking;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SyncStorageFromFsImplTest {
    private FileSystemBlocking fileSystem;
    private FileEntityStorage storage;
    private SyncStorageFromFs uut;

    @Before
    public void setUp() {
        fileSystem = Mockito.mock(FileSystemBlocking.class);
        storage = new InMemoryEntityStorage();
        uut = new SyncStorageFromFsImpl(fileSystem, storage, "downloads");
    }

    @Test
    public void setUsageToFalse() {
        when(fileSystem.readDir("downloads")).thenReturn(Arrays.asList(
                "File._x0x_.part",
                "File._x1x_.part",
                "AnotherFile._x0x_.part",
                "YetAnotherFile._x0x_.part",
                "YetAnotherFile2._x0x_.part"
        ));
        storage.save(new FileEntity("File", 100L, "File._x0x_.part", true));
        storage.save(new FileEntity("File", 100L, "File._x1x_.part", false));
        storage.save(new FileEntity("AnotherFile", 300L, "AnotherFile._x0x_.part", true));
        storage.save(new FileEntity("YetAnotherFile", 500L, "YetAnotherFile._x0x_.part", true));
        storage.save(new FileEntity("YetAnotherFile2", 500L, "YetAnotherFile2._x0x_.part", false));

        uut.execute();

        assertTrue(storage.findByFileName("File._x0x_.part").isPresent());
        assertFalse(storage.findByFileName("File._x0x_.part").get().isInUse());
        assertTrue(storage.findByFileName("File._x1x_.part").isPresent());
        assertFalse(storage.findByFileName("File._x1x_.part").get().isInUse());
        assertTrue(storage.findByFileName("AnotherFile._x0x_.part").isPresent());
        assertFalse(storage.findByFileName("AnotherFile._x0x_.part").get().isInUse());
        assertTrue(storage.findByFileName("YetAnotherFile._x0x_.part").isPresent());
        assertFalse(storage.findByFileName("YetAnotherFile._x0x_.part").get().isInUse());
        assertTrue(storage.findByFileName("YetAnotherFile2._x0x_.part").isPresent());
        assertFalse(storage.findByFileName("YetAnotherFile2._x0x_.part").get().isInUse());
    }

    @Test
    public void remove_non_existent_partials() {
        when(fileSystem.readDir("downloads")).thenReturn(Arrays.asList(
                "/path/to/a/downloads/directory/downloads/File._x0x_.part",
                "/path/to/a/downloads/directory/downloads/AnotherFile._x0x_.part"
        ));

        storage.save(new FileEntity("File", 100L, "File._x0x_.part", true));
        storage.save(new FileEntity("File", 100L, "File._x1x_.part", false));
        storage.save(new FileEntity("AnotherFile", 300L, "AnotherFile._x0x_.part", true));

        uut.execute();

        assertFalse(storage.findByFileName("File._x1x_.part").isPresent());
        assertTrue(storage.findByFileName("File._x0x_.part").isPresent());
        assertTrue(storage.findByFileName("AnotherFile._x0x_.part").isPresent());
    }
}