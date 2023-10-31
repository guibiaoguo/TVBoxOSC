package com.github.tvbox.osc;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.util.AES;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Log.d("TEST","catvod");
        String data = "5ecafb798196ba3aecb1fa2f8f7f3904fa4699259863119e9014d4df8a43b46ff75e3d21fff50166bd4191c9e920a5a1f3da470f09c4c523d176e8faedac3d26d467d23900035248e5a2e17a7a3b2ed86c2f3c56e98c00fffcde5ffbdfbbcfd3";
        String key = "qq1920520460qqzz";
        String iv = "qq1920520460qqzz";
        String value = AES.CBC(data,key,iv);
        Log.d("TEST",value);
//        assertEquals("com.github.catvod", appContext.getPackageName());
    }
}
