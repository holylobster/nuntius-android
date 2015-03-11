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

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import org.holylobster.nuntius.adapter.AppBlacklistAdapter;
import org.holylobster.nuntius.adapter.AppBlacklistAdapter.OnItemClickListener;
import org.holylobster.nuntius.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class AddApplicationBlacklist extends ActionBarActivity {
    private RecyclerView recyclerView;
    private AppBlacklistAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private Toolbar toolbar;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private PackageManager pm;

    private ArrayList<String> blacklistedApp; // Blacklisted App
    private List<ApplicationInfo> packages;   // All Installed App


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist);

        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getResources().getString(R.string.blacklist_title));

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        settings = getSharedPreferences("BlackListSP", MODE_PRIVATE);
        editor = settings.edit();

        pm = getPackageManager();
        packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));

        adapter = new AppBlacklistAdapter(packages, pm);
        recyclerView.setAdapter(adapter);

        blacklistedApp = new ArrayList<>(settings.getStringSet("BlackList", new HashSet<String>()));

        adapter.SetOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                addToBlacklist(position);
            }
        });
    }

    public void addToBlacklist(int position){
        blacklistedApp.add(packages.get(position).packageName);
        editor.putStringSet("BlackList", new HashSet<>(blacklistedApp));
        editor.commit();
        showInfo(position);
    }

    public void showInfo(int position){
        SnackbarManager.show(
            Snackbar.with(getApplicationContext())
                    .text(getString(R.string.added_to_blacklist, pm.getApplicationLabel(packages.get(position)))), this);
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }
}

