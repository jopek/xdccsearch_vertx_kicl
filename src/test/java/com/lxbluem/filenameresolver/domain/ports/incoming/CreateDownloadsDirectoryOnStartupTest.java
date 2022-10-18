package com.lxbluem.filenameresolver.domain.ports.incoming;

import com.lxbluem.filenameresolver.domain.interactors.CreateDownloadsDirectoryOnStartupImpl;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rx.Single;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateDownloadsDirectoryOnStartupTest {
    private FileSystem fileSystem;

    @BeforeEach
    void setUp() throws Exception {
        fileSystem = mock(FileSystem.class);
    }


    @Test
    void create_downloadsDir_because_it_does_not_exist() {
        when(fileSystem.fileOrDirExists("downloads"))
                .thenReturn(Single.just(false));
        when(fileSystem.mkdir("downloads"))
                .thenReturn(Single.just(null));

        new CreateDownloadsDirectoryOnStartupImpl(fileSystem, "downloads")
                .execute();

        verify(fileSystem).mkdir("downloads");
    }

    @Test
    void no_downloadsDir_creation_because_it_exists_already() {
        when(fileSystem.fileOrDirExists("downloads"))
                .thenReturn(Single.just(true));

        new CreateDownloadsDirectoryOnStartupImpl(fileSystem, "downloads").execute();

        verify(fileSystem, never()).mkdir("downloads");
    }


}
