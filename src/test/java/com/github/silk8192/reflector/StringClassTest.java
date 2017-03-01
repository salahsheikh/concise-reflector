package com.github.silk8192.reflector;

import org.junit.Assert;
import org.junit.Test;

import static com.github.silk8192.reflector.Reflector.forClass;

public class StringClassTest {

    @Test
    public void testCreate() {
        Assert.assertTrue(forClass(String.class).create().get() instanceof String);
    }

    @Test
    public void testMethodAccess() {
        Assert.assertEquals((int) forClass(String.class).create("fooBar").invoke("length").get(), 6);
    }
}
