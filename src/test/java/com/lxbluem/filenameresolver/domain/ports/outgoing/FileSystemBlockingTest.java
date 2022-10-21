package com.lxbluem.filenameresolver.domain.ports.outgoing;

import com.lxbluem.filenameresolver.domain.interactors.FileSystemBlockingImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.rxjava.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
class FileSystemBlockingTest {

    private FileSystemBlocking fs;

    @BeforeEach
    void setUp() {
        Vertx vertx = Vertx.vertx();
        fs = new FileSystemBlockingImpl(vertx.fileSystem());
    }

    @Test
    @Disabled("disabled because it expects the downloads folder to exist - debugging helper test")
    void readdir() {
        List<String> downloads = fs.readDir("downloads");
        System.out.println(String.join("\n", downloads));

        assertNotNull(downloads);
    }
}
