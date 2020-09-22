package com.lxbluem.filenameresolver.domain.ports.incoming;

import com.lxbluem.filenameresolver.domain.interactors.CreateDownloadsDirectoryOnStartupImpl;
import com.lxbluem.filenameresolver.domain.ports.outgoing.FileSystem;
import org.junit.Before;
import org.junit.Test;
import rx.Single;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class CreateDownloadsDirectoryOnStartupTest {
    private FileSystem fileSystem;

    @Before
    public void setUp() throws Exception {
        fileSystem = mock(FileSystem.class);
    }


    @Test
    public void create_downloadsDir_because_it_does_not_exist() {
        when(fileSystem.fileOrDirExists("downloads"))
                .thenReturn(Single.just(false));
        when(fileSystem.mkdir("downloads"))
                .thenReturn(Single.just(null));

        new CreateDownloadsDirectoryOnStartupImpl(fileSystem, "downloads")
                .execute();

        verify(fileSystem).mkdir(eq("downloads"));
    }

    @Test
    public void no_downloadsDir_creation_because_it_exists_already() {
        when(fileSystem.fileOrDirExists("downloads"))
                .thenReturn(Single.just(true));

        new CreateDownloadsDirectoryOnStartupImpl(fileSystem, "downloads").execute();

        verify(fileSystem, never()).mkdir(eq("downloads"));
    }


}