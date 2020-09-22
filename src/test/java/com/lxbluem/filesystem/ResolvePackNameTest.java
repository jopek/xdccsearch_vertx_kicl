package com.lxbluem.filesystem;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

//@Ignore
public class ResolvePackNameTest {

    private static final String PATH = "downloads";

    private FilenameResolver resolver;

    @Mock
    private FileSystem fileSystem;

    @Mock
    private FilenameMapper filenameMapper;

    @Captor
    private ArgumentCaptor<Future<List<String>>> fileListFutureCaptor;

    @Captor
    private ArgumentCaptor<Future<Boolean>> booleanFutureCaptor;

    @Captor
    private ArgumentCaptor<Future<FileProps>> filePropsFutureCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        Vertx vertx = mock(Vertx.class);
        when(vertx.fileSystem()).thenReturn(fileSystem);

        filenameMapper = spy(new FilenameMapper());
        resolver = new FilenameResolver(vertx, PATH, filenameMapper);
    }

    @Test
    public void create_downloadsDir_if_not_exists() {
        verify(fileSystem).exists(eq(PATH), booleanFutureCaptor.capture());
        booleanFutureCaptor.getValue().handle(Future.succeededFuture(false));

        verify(fileSystem).mkdir(eq(PATH), any(Future.class));
    }

    @Test
    public void give_valid_new_packname() throws Exception {
        verify(fileSystem).exists(eq(PATH), booleanFutureCaptor.capture());
        booleanFutureCaptor.getValue().handle(Future.succeededFuture(true));

        verify(fileSystem).readDir(eq(PATH), fileListFutureCaptor.capture());
        List<String> filenameList = Arrays.asList(
                "biene maja",
                "biene maja._x1x_",
                "biene maja._x2x_.part"
        );
        fileListFutureCaptor.getValue().handle(Future.succeededFuture(filenameList));

        filenameList.forEach(filename -> {
            verify(fileSystem).props(eq(filename), filePropsFutureCaptor.capture());
            filePropsFutureCaptor.getValue().handle(Future.succeededFuture(mock(FileProps.class)));
        });
        verify(filenameMapper, times(3)).createPackName(anyString());
        verify(filenameMapper, times(3)).createPackSuffix(anyString());


        String requestedPackName = "biene maja";
        Future<String> filesFromDisk = resolver.getFileNameForPackName(requestedPackName);
        filesFromDisk.setHandler(filenameAsyncResult -> {
            assertTrue(filenameAsyncResult.succeeded());

            String filename = filenameAsyncResult.result();
            assertEquals(PATH + "/" + "biene maja._x3x_.part", filename);

            verify(filenameMapper).getFsFilename(eq(PATH + "/" + requestedPackName), eq(3));
        });
    }

    @Test
    public void give_valid_new_packname_file_exists() {
        verify(fileSystem).exists(eq(PATH), booleanFutureCaptor.capture());
        booleanFutureCaptor.getValue().handle(Future.succeededFuture(true));

        String requestedPackName = "biene maja";

        verify(fileSystem).readDir(eq(PATH), fileListFutureCaptor.capture());
        List<String> filenameList = Collections.singletonList(requestedPackName);
        fileListFutureCaptor.getValue().handle(Future.succeededFuture(filenameList));

        filenameList.forEach(filename -> {
            verify(fileSystem).props(eq(filename), filePropsFutureCaptor.capture());
            filePropsFutureCaptor.getValue().handle(Future.succeededFuture(mock(FileProps.class)));
        });

        Future<String> filesFromDisk = resolver.getFileNameForPackName(requestedPackName);
        filesFromDisk.setHandler(filenameAsyncResult -> {
            assertTrue(filenameAsyncResult.succeeded());

            String filename = filenameAsyncResult.result();
            assertEquals(PATH + "/" + "biene maja._x1x_.part", filename);

            verify(filenameMapper).getFsFilename(eq(PATH + "/" + requestedPackName), eq(1));
        });
    }

    @Test
    public void give_valid_new_packname_file_does_not_exist() {
        verify(fileSystem).exists(eq(PATH), booleanFutureCaptor.capture());
        booleanFutureCaptor.getValue().handle(Future.succeededFuture(true));

        String requestedPackName = "biene maja";

        verify(fileSystem).readDir(eq(PATH), fileListFutureCaptor.capture());
        List<String> filenameList = Collections.emptyList();
        fileListFutureCaptor.getValue().handle(Future.succeededFuture(filenameList));

        Future<String> filesFromDisk = resolver.getFileNameForPackName(requestedPackName);
        filesFromDisk.setHandler(filenameAsyncResult -> {
            assertTrue(filenameAsyncResult.succeeded());

            String filename = filenameAsyncResult.result();
            assertEquals(PATH + "/" + "biene maja._x0x_.part", filename);

            verify(filenameMapper).getFsFilename(eq(PATH + "/" + requestedPackName), eq(0));
        });
    }
}