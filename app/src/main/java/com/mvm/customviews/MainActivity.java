package com.mvm.customviews;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RoundRectDrawableWithShadow background = new RoundRectDrawableWithShadow(getResources(),
                Color.parseColor("#8bc34a"),
               0,
                0,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,6.0f,getResources().getDisplayMetrics()),
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,12.0f,getResources().getDisplayMetrics()));
        //background.setAddPaddingForCorners(cardView.getPreventCornerOverlap());
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN)
            findViewById(R.id.text).setBackground(background);
        else
            findViewById(R.id.text).setBackgroundDrawable(background);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
