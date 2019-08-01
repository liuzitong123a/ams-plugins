package com.kwunai.app.asmplugin;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.kwunai.asm.annotation.VitalTime;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        long var1 = System.currentTimeMillis();
        long var3 = System.currentTimeMillis();
        System.out.println("execute:" + (var3 - var1) + "ms.");
//        test();
    }

    @VitalTime
    private void test() {
        Log.e("lzt", "xxx");
    }
}
