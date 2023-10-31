package com.github.tvbox.osc.api;

import static org.junit.Assert.*;

import com.github.tvbox.osc.util.AES;

import org.junit.Assert;
import org.junit.Test;

public class ApiConfigTest {

    @Test
    public void getX() {
        System.out.println("TEST");
        Assert.assertTrue(true);
    }

    @Test
    public void cbc() throws Exception {
        String data = "5ecafb798196ba3aecb1fa2f8f7f3904fa4699259863119e9014d4df8a43b46ff75e3d21fff50166bd4191c9e920a5a1f3da470f09c4c523d176e8faedac3d26d467d23900035248e5a2e17a7a3b2ed86c2f3c56e98c00fffcde5ffbdfbbcfd3";
        String key = "qq1920520460qqzz";
        String iv = "qq1920520460qqzz";
        String value = AES.CBC(data,key,iv);
        System.out.println(value);
    }
}