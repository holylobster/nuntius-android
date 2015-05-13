/*
 * Copyright (C) 2015 - Holy Lobster
 *
 * Nuntius is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Nuntius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Nuntius. If not, see <http://www.gnu.org/licenses/>.
 */

package org.holylobster.nuntius.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import org.holylobster.nuntius.R;


public class SplashScreenActivity extends Activity implements Runnable {

    private static final String debugTag = "SplashScreenActivity";

    private boolean gotIt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get SharedPreferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        gotIt = settings.getBoolean("gotIt", false);
        boolean enabledSplash = settings.getBoolean("enable_splash", true);

        //If enabledSplash is true show Splash else launch the next Activity
        if (enabledSplash) {
            setContentView(R.layout.activity_splash);
            ((GradientDrawable) findViewById(R.id.splash).getBackground().getCurrent())
                    .setGradientRadius(getResources().getDimension(
                            R.dimen.gradient_radius));
            new Handler().postDelayed(this, 2000);
        } else {
            launchNextActivity();
        }
    }

    /**
     * Launch the next Activity
     * Launch WelcomeActivity if gotIt is false;
     * Launch SettingsActivity if gotit is true;
     */
    private void launchNextActivity() {
        Class activityClass = gotIt ? SettingsActivity.class : WelcomeActivity.class;
        Intent i = new Intent(SplashScreenActivity.this, activityClass);
        startActivity(i);
        finish();
    }

    @Override
    public void run() {
        launchNextActivity();
    }
}
