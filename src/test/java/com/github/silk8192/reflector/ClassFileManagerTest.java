package com.github.silk8192.reflector;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ClassFileManagerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    @Test
    public void addAllFromTest() throws IOException, URISyntaxException {
        Class<?> s = getClass(); // in this test, we get our own class
        Path k = Paths.get(s.getProtectionDomain().getCodeSource().getLocation().toURI());  // find our where it's located
        ClassFileManager cfm = new ClassFileManager(k);
        Class<?> c = cfm.getClass(this.getClass().getCanonicalName()); // see if we can find a class by our own name
        Assert.assertEquals(s, c); // if we can find our own class, it's a success!
    }
}
