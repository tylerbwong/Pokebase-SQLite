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

package me.tylerbwong.pokebase.gui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.fabtransitionactivity.SheetLayout;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tylerbwong.pokebase.R;
import me.tylerbwong.pokebase.gui.adapters.PokemonTeamMemberAdapter;
import me.tylerbwong.pokebase.gui.views.AnimatedRecyclerView;
import me.tylerbwong.pokebase.model.components.PokemonTeamMember;
import me.tylerbwong.pokebase.model.database.DatabaseOpenHelper;

/**
 * @author Tyler Wong
 */
public class TeamViewActivity extends AppCompatActivity implements SheetLayout.OnFabAnimationEndListener {
   @BindView(R.id.toolbar)
   Toolbar mToolbar;
   @BindView(R.id.fab)
   FloatingActionButton mFab;
   @BindView(R.id.bottom_sheet)
   SheetLayout mSheetLayout;
   @BindView(R.id.team_list)
   AnimatedRecyclerView mPokemonList;
   @BindView(R.id.empty_layout)
   LinearLayout mEmptyView;
   @BindView(R.id.no_team)
   ImageView mNoTeam;
   @BindView(R.id.no_team_label)
   TextView mNoTeamLabel;
   @BindView(R.id.name_input)
   TextInputEditText mNameInput;
   @BindView(R.id.description_input)
   TextInputEditText mDescriptionInput;

   private ActionBar mActionBar;

   private PokemonTeamMemberAdapter mPokemonAdapter;
   private PokemonTeamMember[] mPokemon;
   private DatabaseOpenHelper mDatabaseHelper;

   private boolean mUpdateKey;
   private int mTeamId;
   private FirebaseAnalytics mAnalytics;

   private static final String DEFAULT_NAME = "Team ";
   private static final String DEFAULT_DESCRIPTION = "None";
   private static final String TEAM_CREATED = "team_created";
   private static final String CREATED_TEAM = "created_team";
   public static final String TEAM_ID_KEY = "team_id_key";
   public static final String UPDATE_KEY = "update_key";
   public static final String TEAM_NAME = "teamName";
   public static final String DESCRIPTION = "description";
   public static final String POKEMON_ADD = "pokemonAdd";
   private static final int MAX_TEAM_SIZE = 6;
   private static final int REQUEST_CODE = 1;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_team);
      ButterKnife.bind(this);

      mDatabaseHelper = DatabaseOpenHelper.getInstance(this);
      mAnalytics = FirebaseAnalytics.getInstance(this);

      Glide.with(this)
            .load(R.drawable.no_teams)
            .into(mNoTeam);
      mNoTeamLabel.setText(getString(R.string.no_pokemon));

      mPokemonList.setHasFixedSize(true);
      LinearLayoutManager layoutManager = new LinearLayoutManager(this);
      layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
      mPokemonList.setLayoutManager(layoutManager);

      mSheetLayout.setFab(mFab);
      mSheetLayout.setFabAnimationEndListener(this);

      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      mTeamId = extras.getInt(TEAM_ID_KEY);
      mUpdateKey = extras.getBoolean(UPDATE_KEY, false);
      String teamTitle = extras.getString(TEAM_NAME, DEFAULT_NAME
            + (mDatabaseHelper.queryLastTeamAddedId() + 1));
      String description = extras.getString(DESCRIPTION, DEFAULT_DESCRIPTION);

      setSupportActionBar(mToolbar);
      mActionBar = getSupportActionBar();

      if (mActionBar != null) {
         mActionBar.setDisplayHomeAsUpEnabled(true);
         mActionBar.setTitle(teamTitle);
      }

      mNameInput.setText(teamTitle);
      mDescriptionInput.setText(description);

      mPokemonAdapter = new PokemonTeamMemberAdapter(this, null, mTeamId,
            mNameInput.getText().toString().trim(),
            mDescriptionInput.getText().toString().trim());
      mPokemonList.setAdapter(mPokemonAdapter);

      if (mUpdateKey) {
         mDatabaseHelper.queryPokemonTeamMembers(mTeamId)
               .subscribeOn(Schedulers.newThread())
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(pokemonTeamMembers -> {
                     mPokemon = pokemonTeamMembers;
                     mPokemonAdapter.setTeam(mPokemon);

                     if (mPokemon.length == MAX_TEAM_SIZE) {
                        ViewGroup viewGroup = (ViewGroup) mFab.getParent();
                        viewGroup.removeView(mFab);
                     }

                     checkEmpty();
               });
      }
      else {
         if (mActionBar != null) {
            mActionBar.setTitle(R.string.new_team);
         }
      }
   }

   @OnTextChanged(R.id.name_input)
   public void textChanged(CharSequence sequence) {
      if (mActionBar != null) {
         mActionBar.setTitle(sequence.toString());

         if (!mUpdateKey && sequence.toString().trim().length() == 0) {
            mActionBar.setTitle(R.string.new_team);
         }
      }
   }

   @OnClick(R.id.fab)
   public void onFabClick() {
      if (addTeam()) {
         mSheetLayout.expandFab();
      }
   }

   @Override
   public void onFabAnimationEnd() {
      Intent intent = new Intent(this, MainActivity.class);
      Bundle extras = new Bundle();
      if (mTeamId == 0) {
         mTeamId = mDatabaseHelper.queryLastTeamAddedId();
      }
      extras.putInt(TEAM_ID_KEY, mTeamId);
      extras.putBoolean(UPDATE_KEY, true);
      extras.putString(TEAM_NAME, mNameInput.getText().toString().trim());
      extras.putString(DESCRIPTION, mDescriptionInput.getText().toString().trim());
      extras.putBoolean(POKEMON_ADD, true);
      intent.putExtras(extras);
      startActivityForResult(intent, REQUEST_CODE);
   }

   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == REQUEST_CODE) {
         mSheetLayout.contractFab();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu_trash, menu);
      inflater.inflate(R.menu.menu_submit, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case android.R.id.home:
            onBackPressed();
            break;
         case R.id.delete_action:
            showDeleteDialog();
            break;
         case R.id.submit_action:
            if (addTeam()) {
               backToMain();
            }
            break;
         default:
            break;
      }
      return true;
   }

   private boolean addTeam() {
      String name = mNameInput.getText().toString().trim();
      String description = mDescriptionInput.getText().toString().trim();
      boolean doesTeamNameExist = mDatabaseHelper.doesTeamNameExist(name, mTeamId, mUpdateKey);
      boolean result = false;

      if (!mUpdateKey && !doesTeamNameExist) {
         if (name.length() == 0) {
            name = DEFAULT_NAME + (mDatabaseHelper.queryLastTeamAddedId() + 1);
         }
         if (description.length() == 0) {
            description = DEFAULT_DESCRIPTION;
         }
         mDatabaseHelper.insertTeam(name, description);
         Toast.makeText(this, String.format(getString(R.string.team_add), name),
               Toast.LENGTH_SHORT).show();
         result = true;

         Bundle bundle = new Bundle();
         bundle.putBoolean(TEAM_CREATED, true);
         mAnalytics.logEvent(CREATED_TEAM, bundle);
      }
      else if (mUpdateKey && !doesTeamNameExist) {
         mDatabaseHelper.updateTeam(mTeamId, name, description);
         Toast.makeText(this, String.format(getString(R.string.team_update), name),
               Toast.LENGTH_SHORT).show();
         result = true;
      }
      else {
         showUsedNameDialog();
      }

      return result;
   }

   private void showUsedNameDialog() {
      new LovelyStandardDialog(this)
            .setIcon(R.drawable.ic_info_white_24dp)
            .setTitle(R.string.used_name)
            .setMessage(String.format(getString(R.string.used_name_info),
                  mNameInput.getText().toString().trim()))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.ok), null)
            .setTopColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .show();
   }

   private void showDeleteDialog() {
      new LovelyStandardDialog(this)
            .setIcon(R.drawable.ic_info_white_24dp)
            .setTitle(R.string.delete_team)
            .setMessage(String.format(getString(R.string.delete_team_prompt),
                  mNameInput.getText().toString().trim()))
            .setCancelable(true)
            .setPositiveButton(R.string.yes, v -> deleteTeam())
            .setNegativeButton(R.string.no, null)
            .setTopColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .show();
   }

   private void deleteTeam() {
      mDatabaseHelper.deleteTeamPokemonAll(mTeamId)
         .concatWith(mDatabaseHelper.deleteTeam(mTeamId))
         .subscribeOn(Schedulers.newThread())
         .observeOn(AndroidSchedulers.mainThread())
         .subscribe(() -> {
            Toast.makeText(this, String.format(getString(R.string.team_deleted),
                    mNameInput.getText().toString().trim()), Toast.LENGTH_SHORT).show();
            backToMain();
         });
   }

   private void showBackDialog() {
      new LovelyStandardDialog(this)
            .setIcon(R.drawable.ic_info_white_24dp)
            .setTitle(R.string.go_back)
            .setMessage(R.string.back_prompt)
            .setCancelable(true)
            .setPositiveButton(R.string.yes, v -> backToMain())
            .setNegativeButton(R.string.no, null)
            .setTopColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .show();
   }

   private void backToMain() {
      startActivity(new Intent(this, MainActivity.class));
   }

   private void checkEmpty() {
      if (mPokemon == null || mPokemon.length == 0) {
         mEmptyView.setVisibility(View.VISIBLE);
      }
      else {
         mEmptyView.setVisibility(View.INVISIBLE);
      }
   }

   @Override
   public void onBackPressed() {
      showBackDialog();
   }
}
