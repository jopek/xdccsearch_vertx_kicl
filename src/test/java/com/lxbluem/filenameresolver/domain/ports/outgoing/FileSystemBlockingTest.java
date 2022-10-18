package com.lxbluem.filenameresolver.domain.ports.outgoing;

import com.lxbluem.filenameresolver.domain.interactors.FileSystemBlockingImpl;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.ext.unit.TestContext;
import io.vertx.rxjava.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
class FileSystemBlockingTest {

    private FileSystemBlocking fs;

    @BeforeEach
    void setUp(TestContext context) {
        Vertx vertx = Vertx.vertx();
        fs = new FileSystemBlockingImpl(vertx.fileSystem());
    }

    @Test
    void readdir() {
        List<String> downloads = fs.readDir("downloads");
        System.out.println(String.join("\n", downloads));

        assertNotNull(downloads);
    }
}
