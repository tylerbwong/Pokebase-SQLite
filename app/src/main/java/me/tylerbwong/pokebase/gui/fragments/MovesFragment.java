/*
 * Copyright 2016 Tyler Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tylerbwong.pokebase.gui.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import me.tylerbwong.pokebase.R;
import me.tylerbwong.pokebase.gui.adapters.MoveListAdapter;
import me.tylerbwong.pokebase.gui.adapters.TextViewSpinnerAdapter;
import me.tylerbwong.pokebase.model.database.DatabaseOpenHelper;

/**
 * @author Tyler Wong
 */
public class MovesFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    @BindView(R.id.type_spinner)
    Spinner typeSpinner;
    @BindView(R.id.class_spinner)
    Spinner classSpinner;
    @BindView(R.id.moves_list)
    RecyclerView movesList;

    private DatabaseOpenHelper databaseHelper;
    private Unbinder unbinder;
    private String[] moves;

    private static final String TYPES = "Types";
    private static final String CLASSES = "Classes";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTheme(R.style.PokemonEditorTheme);
        databaseHelper = DatabaseOpenHelper.getInstance(getContext());
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTheme(R.style.PokemonEditorTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.moves_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.moves);
        }

        typeSpinner.setOnItemSelectedListener(this);
        classSpinner.setOnItemSelectedListener(this);

        movesList.setLayoutManager(new LinearLayoutManager(getContext()));
        movesList.setHasFixedSize(true);

        new LoadMoveList().execute();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.clear_all_teams_action).setVisible(false);
        menu.findItem(R.id.number_action).setVisible(false);
        menu.findItem(R.id.name_action).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        refreshData();
    }

    private void refreshData() {
        String type = (String) typeSpinner.getSelectedItem();
        String className = (String) classSpinner.getSelectedItem();

        new RefreshData().execute(type, className);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private class RefreshData extends AsyncTask<String, Void, String[]> {
        @Override
        protected String[] doInBackground(String... params) {
            String type = params[0];
            String className = params[1];

            if (type.equals(TYPES) && !className.equals(CLASSES)) {
                moves = databaseHelper.queryMovesByClass(className);
            }
            else if (!type.equals(TYPES) && className.equals(CLASSES)) {
                moves = databaseHelper.queryMovesByType(type);
            }
            else if (!type.equals(TYPES) && !className.equals(CLASSES)) {
                moves = databaseHelper.queryMovesByTypeAndClass(type, className);
            }
            else {
                moves = databaseHelper.queryAllMoves();
            }

            return moves;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            movesList.setAdapter(new MoveListAdapter(getContext(), moves));
        }
    }

    private class LoadMoveList extends AsyncTask<Void, Void, Pair<Pair<String[], String[]>, String[]>> {
        @Override
        protected Pair<Pair<String[], String[]>, String[]> doInBackground(Void... params) {
            String[] types = databaseHelper.queryAllTypes();
            String[] classes = databaseHelper.queryAllClasses();
            String[] moves = databaseHelper.queryAllMoves();

            return new Pair<>(new Pair<>(types, classes), moves);
        }

        @Override
        protected void onPostExecute(Pair<Pair<String[], String[]>, String[]> result) {
            super.onPostExecute(result);
            typeSpinner.setAdapter(new TextViewSpinnerAdapter(getContext(), result.first.first));
            classSpinner.setAdapter(new TextViewSpinnerAdapter(getContext(), result.first.second));
            movesList.setAdapter(new MoveListAdapter(getContext(), result.second));
        }
    }
}
