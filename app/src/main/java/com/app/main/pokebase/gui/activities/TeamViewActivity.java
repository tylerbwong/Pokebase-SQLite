package com.app.main.pokebase.gui.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.main.pokebase.R;
import com.app.main.pokebase.gui.adapters.PokemonTeamMemberAdapter;
import com.app.main.pokebase.gui.views.AnimatedRecyclerView;
import com.app.main.pokebase.model.components.PokemonTeamMember;
import com.app.main.pokebase.model.database.DatabaseOpenHelper;
import com.github.fabtransitionactivity.SheetLayout;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

/**
 * @author Tyler Wong
 */
public class TeamViewActivity extends AppCompatActivity implements SheetLayout.OnFabAnimationEndListener {
   private Toolbar mToolbar;
   private FloatingActionButton mFab;
   private SheetLayout mSheetLayout;
   private AnimatedRecyclerView mPokemonList;
   private LinearLayout mEmptyView;
   private ImageView mNoTeam;
   private TextView mNoTeamLabel;
   private TextInputEditText mNameInput;
   private TextInputEditText mDescriptionInput;

   private PokemonTeamMemberAdapter mPokemonAdapter;
   private PokemonTeamMember[] mPokemon;
   private DatabaseOpenHelper mDatabaseHelper;

   private boolean mUpdateKey;
   private int mTeamId;

   private final static String DEFAULT_NAME = "Team ";
   private final static String DEFAULT_DESCRIPTION = "None";
   public static final String TEAM_ID_KEY = "team_id_key";
   public static final String UPDATE_KEY = "update_key";
   public final static String TEAM_NAME = "teamName";
   public final static String DESCRIPTION = "description";
   public final static String POKEMON_ADD = "pokemonAdd";
   private final static int MAX_TEAM_SIZE = 6;
   private final static int REQUEST_CODE = 1;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_team);

      mToolbar = (Toolbar) findViewById(R.id.toolbar);
      mFab = (FloatingActionButton) findViewById(R.id.fab);
      mSheetLayout = (SheetLayout) findViewById(R.id.bottom_sheet);
      mPokemonList = (AnimatedRecyclerView) findViewById(R.id.team_list);
      mEmptyView = (LinearLayout) findViewById(R.id.empty_layout);
      mNoTeam = (ImageView) findViewById(R.id.no_team);
      mNoTeamLabel = (TextView) findViewById(R.id.no_team_label);
      mNameInput = (TextInputEditText) findViewById(R.id.name_input);
      mDescriptionInput = (TextInputEditText) findViewById(R.id.description_input);

      new LoadNoTeamMembersDrawable(this).execute();

      mDatabaseHelper = DatabaseOpenHelper.getInstance(this);
      mPokemonList.setHasFixedSize(true);

      mFab.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            onFabClick();
         }
      });

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
      final ActionBar actionBar = getSupportActionBar();

      if (actionBar != null) {
         actionBar.setDisplayHomeAsUpEnabled(true);
         actionBar.setTitle(teamTitle);
      }

      mNameInput.addTextChangedListener(new TextWatcher() {

         @Override
         public void onTextChanged(CharSequence sequence, int start, int before, int count) {
            if (actionBar != null) {
               actionBar.setTitle(sequence.toString());

               if (!mUpdateKey && sequence.toString().trim().length() == 0) {
                  actionBar.setTitle(R.string.new_team);
               }
            }
         }

         @Override
         public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {

         }

         @Override
         public void afterTextChanged(Editable editable) {
         }
      });

      mNameInput.setText(teamTitle);
      mDescriptionInput.setText(description);

      if (mUpdateKey) {
         new LoadTeamMembers(this).execute();
      }
      else {
         if (actionBar != null) {
            actionBar.setTitle(R.string.new_team);
         }
      }
   }

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
            .setPositiveButton(R.string.yes, new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                  deleteTeam();
               }
            })
            .setNegativeButton(R.string.no, null)
            .setTopColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .show();
   }

   private void deleteTeam() {
      mDatabaseHelper.deleteTeamPokemonAll(mTeamId);
      mDatabaseHelper.deleteTeam(mTeamId);

      Toast.makeText(this, String.format(getString(R.string.team_deleted),
            mNameInput.getText().toString().trim()), Toast.LENGTH_SHORT).show();
      backToMain();
   }

   private void showBackDialog() {
      new LovelyStandardDialog(this)
            .setIcon(R.drawable.ic_info_white_24dp)
            .setTitle(R.string.go_back)
            .setMessage(R.string.back_prompt)
            .setCancelable(true)
            .setPositiveButton(R.string.yes, new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                  backToMain();
               }
            })
            .setNegativeButton(R.string.no, null)
            .setTopColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .show();
   }

   private void backToMain() {
      Intent mainIntent = new Intent(this, MainActivity.class);
      startActivity(mainIntent);
   }

   @Override
   public void onBackPressed() {
      showBackDialog();
   }

   private class LoadNoTeamMembersDrawable extends AsyncTask<Void, Void, Drawable> {
      private Context mContext;

      public LoadNoTeamMembersDrawable(Context context) {
         this.mContext = context;
      }

      @Override
      protected Drawable doInBackground(Void... params) {
         return ContextCompat.getDrawable(mContext, R.drawable.no_teams);
      }

      @Override
      protected void onPostExecute(Drawable loaded) {
         super.onPostExecute(loaded);

         mNoTeam.setImageDrawable(loaded);
         mNoTeamLabel.setText(getString(R.string.no_pokemon));
      }
   }

   private class LoadTeamMembers extends AsyncTask<Void, Void, PokemonTeamMember[]> {
      private Context mContext;

      public LoadTeamMembers(Context context) {
         this.mContext = context;
      }

      @Override
      protected PokemonTeamMember[] doInBackground(Void... params) {
         return mPokemon = mDatabaseHelper.queryPokemonTeamMembers(mTeamId);
      }

      @Override
      protected void onPostExecute(PokemonTeamMember[] loaded) {
         super.onPostExecute(loaded);

         mPokemonAdapter = new PokemonTeamMemberAdapter(mContext, loaded, mTeamId,
               mNameInput.getText().toString().trim(),
               mDescriptionInput.getText().toString().trim());
         mPokemonList.setAdapter(mPokemonAdapter);

         if (loaded.length == MAX_TEAM_SIZE) {
            ViewGroup viewGroup = (ViewGroup) mFab.getParent();
            viewGroup.removeView(mFab);
         }

         LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
         layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
         mPokemonList.setLayoutManager(layoutManager);

         if (mPokemon == null || mPokemon.length == 0) {
            mPokemonList.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
         }
         else {
            mPokemonList.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
         }
      }
   }
}
