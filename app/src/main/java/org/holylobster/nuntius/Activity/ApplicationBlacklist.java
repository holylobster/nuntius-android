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

package org.holylobster.nuntius.Activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import org.holylobster.nuntius.AppBlacklistAdapter;
import org.holylobster.nuntius.R;

import java.util.Collections;
import java.util.List;

/**
 * Created by fly on 12/03/15.
 */
public class ApplicationBlacklist extends Activity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist);

        ActionBar mActionBar = getActionBar();
        //mActionBar.setDisplayShowHomeEnabled(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm)); // Sort by App name
        Drawable[] icons = new Drawable[packages.size()];
        String[] packageName = new String[packages.size()];

        for (int i = 0; i< icons.length; i++){
            icons[i] = pm.getApplicationIcon(packages.get(i));
        }
        for (int i = 0; i< packageName.length; i++){
            packageName[i] = (String) pm.getApplicationLabel(packages.get(i));
        }

        mAdapter = new AppBlacklistAdapter(packageName, icons);
        mRecyclerView.setAdapter(mAdapter);

    }
}
