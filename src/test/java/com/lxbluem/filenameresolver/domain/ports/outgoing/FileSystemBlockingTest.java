package com.lxbluem.filenameresolver.domain.ports.outgoing;

import com.lxbluem.filenameresolver.domain.interactors.FileSystemBlockingImpl;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(VertxUnitRunner.class)
public class FileSystemBlockingTest {

    private FileSystemBlocking fs;

    @Before
    public void setUp(TestContext context) {
        Vertx vertx = Vertx.vertx();
        fs = new FileSystemBlockingImpl(vertx.fileSystem());
    }

    @Test
    public void readdir() {
        List<String> downloads = fs.readDir("downloads");
        System.out.println(String.join("\n", downloads));
    }
}