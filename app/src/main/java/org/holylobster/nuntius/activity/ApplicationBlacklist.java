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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;

import org.holylobster.nuntius.adapter.AppBlacklistAdapter;
import org.holylobster.nuntius.R;

import java.util.ArrayList;
import java.util.HashSet;


public class ApplicationBlacklist extends ActionBarActivity {
    private RecyclerView recyclerView;
    private AppBlacklistAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private Toolbar toolbar;

    private ArrayList<String> blacklistedApp;

    private SharedPreferences settings;
    SharedPreferences.Editor editor;
    PackageManager pm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist);

        toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getResources().getString(R.string.blacklisted_title));

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        pm = getPackageManager();
        settings = getSharedPreferences("BlackListSP", MODE_PRIVATE);
        editor = settings.edit();

        blacklistedApp = new ArrayList<>(settings.getStringSet("BlackList", new HashSet<String>()));

        adapter = new AppBlacklistAdapter(blacklistedApp, pm);
        recyclerView.setAdapter(adapter);
        adapter.SetOnItemClickListener(new AppBlacklistAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                itemSelected(position);
            }
        });
    }

    public void itemSelected(int i){
        String oldApp = blacklistedApp.get(i);
        blacklistedApp.remove(i);
        editor.putStringSet("BlackList", new HashSet<>(blacklistedApp));
        editor.commit();
        adapter.refresh(blacklistedApp);
        showInfo(oldApp);
    }

    public void showInfo(final String app){
        try {
            SnackbarManager.show(
                    Snackbar.with(getApplicationContext())
                            .actionColor(getResources().getColor(R.color.red))
                            .actionLabel("Undo") // action button label
                            .actionListener(new ActionClickListener() {
                                @Override
                                public void onActionClicked(Snackbar snackbar) {
                                    blacklistedApp.add(app);
                                    editor.putStringSet("BlackList", new HashSet<>(blacklistedApp));
                                    editor.commit();
                                    adapter.refresh(blacklistedApp);
                                }
                            }) // action button's ActionClickListener
                            .text(pm.getApplicationLabel(pm.getApplicationInfo(app, 0)) + " " + getResources().getString(R.string.removed_from_blacklist)), this);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.blacklist_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.add:
                Intent intent = new Intent(this, AddApplicationBlacklist.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

