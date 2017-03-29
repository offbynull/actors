package com.offbynull.actors.core.checkpoint;

import com.offbynull.actors.core.context.ObjectStreamSerializer;
import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class FileSystemCheckpointerTest {

    public FileSystemCheckpointer fixture;
    public Path path;
    
    @Before
    public void before() {
        path = Paths.get("fscp_test");
        fixture = FileSystemCheckpointer.create(new ObjectStreamSerializer(), path);
    }
    
    @After
    public void after() throws Exception {
        fixture.close();
        FileUtils.deleteDirectory(path.toFile());
    }

    @Test
    public void mustSaveAndRestoreContext() throws Exception{
        SourceContext ctxIn = new SourceContext(new CoroutineRunner((Coroutine & Serializable) cnt -> {}), Address.fromString("test1:test2"));
        fixture.checkpoint(ctxIn).get();
        
        try (RestoreResultIterator it = fixture.restore()) {
            SourceContext ctxOut = it.next();
            assertEquals(ctxIn.self(), ctxOut.self());
        }
    }
    
}
