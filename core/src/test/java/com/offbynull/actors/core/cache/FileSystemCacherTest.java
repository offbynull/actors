package com.offbynull.actors.core.cache;

import com.offbynull.actors.core.context.ObjectStreamSerializer;
import com.offbynull.actors.core.context.SourceContext;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;


public class FileSystemCacherTest {
    
    public FileSystemCacher fixture;
    public Path path;
    
    @Before
    public void before() throws Exception {
        path = Files.createTempDirectory("fsc_test");
        fixture = FileSystemCacher.create(new ObjectStreamSerializer(), path);
    }
    
    @After
    public void after() throws Exception {
        fixture.close();
        FileUtils.deleteDirectory(path.toFile());
    }

    @Test
    public void mustSaveAndRestoreContext() throws Exception{
        Address self = Address.fromString("test1:test2");
        SourceContext ctxIn = new SourceContext(new CoroutineRunner((Coroutine & Serializable) cnt -> {}), self);
        boolean cached = fixture.save(ctxIn);
        assertTrue(cached);
        
        SourceContext ctxOut = fixture.restore(self);
        assertEquals(ctxIn.self(), ctxOut.self());
    }
    
}
