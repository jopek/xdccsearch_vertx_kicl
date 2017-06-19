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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FilenameMemDbTest {

    private static final String PATH = "d";
    private FilenameMemDb memDb;

    @Mock
    private FileSystem fileSystem;

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

        memDb = new FilenameMemDb(vertx, PATH, new FilenameMapper());

        verify(fileSystem).exists(eq(PATH), booleanFutureCaptor.capture());
        booleanFutureCaptor.getValue().handle(Future.succeededFuture(true));

    }

    @Test
    public void packnames() throws Exception {
        verify(fileSystem).readDir(eq(PATH), fileListFutureCaptor.capture());
        List<String> objects = Arrays.asList("d/a", "d/a._1_", "d/b", "d/c");
        fileListFutureCaptor.getValue().handle(Future.succeededFuture(objects));

        objects.forEach(filename -> {
            verify(fileSystem).props(eq(filename), filePropsFutureCaptor.capture());
            filePropsFutureCaptor.getValue().handle(Future.succeededFuture(mock(FileProps.class)));
        });


        Future<List<FileEntity>> filesFromDisk = memDb.getPackFilesFromDisk("a");

        filesFromDisk.setHandler(h -> {
            assertTrue(h.succeeded());

            List<FileEntity> result = h.result();
            assertEquals(2, result.size());

            FileEntity fileEntity;
            fileEntity = result.get(0);
            assertEquals("d/a", fileEntity.getPackname());
            assertEquals(0, fileEntity.getSuffix());

            fileEntity = result.get(1);
            assertEquals("d/a", fileEntity.getPackname());
            assertEquals(1, fileEntity.getSuffix());
        });
    }

    @Test
    public void give_valid_new_packname() throws Exception {
        verify(fileSystem).readDir(eq(PATH), fileListFutureCaptor.capture());
        List<String> objects = Arrays.asList("d/a", "d/a._1_");
        fileListFutureCaptor.getValue().handle(Future.succeededFuture(objects));

        objects.forEach(filename -> {
            verify(fileSystem).props(eq(filename), filePropsFutureCaptor.capture());
            filePropsFutureCaptor.getValue().handle(Future.succeededFuture(mock(FileProps.class)));
        });


        Future<String> filesFromDisk = memDb.getPackFilesName("a");

        filesFromDisk.setHandler(h -> {
            assertTrue(h.succeeded());

            String result = h.result();

            assertEquals("d/a._2_", result);
        });
    }
}