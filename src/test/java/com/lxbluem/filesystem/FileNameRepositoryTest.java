package com.lxbluem.filesystem;

import com.lxbluem.filesystem.repository.SimpleCrudRepository;
import com.lxbluem.filesystem.repository.impl.FilenameRepositoryImpl;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class FileNameRepositoryTest {

    private Vertx vertx;
    private SimpleCrudRepository<FileEntity, FileEntity> fileNameRepository;

    @BeforeClass
    public static void setupClass() {
        System.setProperty("sqlite.purejava", "true");
    }

    @Before
    public void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();
        vertx.exceptionHandler(context.exceptionHandler());
        fileNameRepository = new FilenameRepositoryImpl(vertx, "test.db");
    }

    @After
    public void tearDown() throws Exception {
        vertx.close();
    }

    @Test
    public void create_db_on_init(TestContext context) throws Exception {
        Async async = context.async();

        fileNameRepository.retrieveAll().subscribe(
                entity -> {
                    async.complete();
                    context.assertEquals(null, entity.getPackname());
                },
                onError -> {
                    async.complete();
                    System.out.println("OnError in test");
                    context.fail(onError);
                }
        );
        async.await();
    }

//    @Test
//    public void add_entry(TestContext context) throws Exception {
//        Async async = context.async();
//
//
//
//        fileNameRepository.put().subscribe(
//                resultSet -> {
//                    async.complete();
//                    context.assertEquals(0, resultSet.si());
//                    context.assertEquals(4, resultSet.getNumColumns());
//                },
//                onError -> {
//                    async.complete();
//                    System.out.println("OnError in test");
//                    context.fail(onError);
//                }
//        );
//        async.await();
//    }
}