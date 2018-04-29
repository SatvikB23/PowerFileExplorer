package com.amaze.filemanager.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import net.gnu.explorer.R;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;
import com.amaze.filemanager.utils.PreferenceUtils;
import com.amaze.filemanager.utils.color.ColorUsage;
import com.amaze.filemanager.utils.theme.AppTheme;
import java.util.ArrayList;
import com.amaze.filemanager.filesystem.BaseFile;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.CopyService;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.services.asynctasks.MoveFiles;
import android.app.Activity;
import com.amaze.filemanager.utils.MainActivityHelper;
import com.amaze.filemanager.utils.OpenMode;

/**
 * Created by arpitkh996 on 03-03-2016.
 */
public class ThemedActivity extends BasicActivity {
    public SharedPreferences sharedPref;

    public static boolean rootMode;
    boolean checkStorage = true;

    // oppathe - the path at which certain operation needs to be performed
    // oppathe1 - the new path which user wants to create/modify
    // oppathList - the paths at which certain operation needs to be performed (pairs with oparrayList)
    public String oppathe, oppathe1;
	public ArrayList<String> oppatheList;
	
    public int operation = -1;
    public ArrayList<BaseFile> oparrayList;
    public ArrayList<ArrayList<BaseFile>> oparrayListList;
	public final static int FROM_PREVIOUS_IO_ACTION = 3;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // checking if theme should be set light/dark or automatic
        if (sharedPref.getBoolean("random_checkbox", false)) {
            getColorPreference().randomize()
				.saveToPreferences(sharedPref);
        }

        setTheme();

        rootMode = sharedPref.getBoolean(PreferenceUtils.KEY_ROOT, false);

        //requesting storage permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkStorage)
            if (!checkStoragePermission())
                requestStoragePermission();
    }


	@Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
		Log.d("ThemedActivity", "onActivityResult: " + requestCode + ", " + intent);

		if (requestCode == FROM_PREVIOUS_IO_ACTION) {
			Uri treeUri;
			if (responseCode == Activity.RESULT_OK) {
				// Get Uri from Storage Access Framework.
				treeUri = intent.getData();
				// Persist URI - this is required for verification of writability.
				if (treeUri != null) sharedPref.edit().putString("URI", treeUri.toString()).commit();
			} else {
				// If not confirmed SAF, or if still not writable, then revert settings.
				/* DialogUtil.displayError(getActivity(), R.string.message_dialog_cannot_write_to_folder_saf, false, currentFolder);
				 ||!FileUtil.isWritableNormalOrSaf(currentFolder)*/
				return;
			}

			// After confirmation, update stored value of folder.
			// Persist access permissions.

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
																  | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			}
			switch (operation) {
				case DataUtils.DELETE://deletion
					new DeleteTask(null, this).execute((oparrayList));
					break;
				case DataUtils.RENAME:
                    MainActivityHelper.rename(OpenMode.FILE, (oppathe),
											  (oppathe1), this, ThemedActivity.rootMode);
                    break;
			}
			operation = -1;
        } 
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public boolean checkStoragePermission() {

        // Verify that all required contact permissions have been granted.
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
			== PackageManager.PERMISSION_GRANTED;
    }

    protected void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
																Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.
            final MaterialDialog materialDialog = GeneralDialogCreation.showBasicDialog(this,
																						new String[]{getString(R.string.granttext),
																							getString(R.string.grantper),
																							getString(R.string.grant),
																							getString(R.string.cancel),
																							null});
            materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						ActivityCompat
                            .requestPermissions(ThemedActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 77);
						materialDialog.dismiss();
					}
				});
            materialDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
					}
				});
            materialDialog.setCancelable(false);
            materialDialog.show();

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 77);
        }
    }

    void setTheme() {
        AppTheme theme = getAppTheme().getSimpleTheme();
        if (Build.VERSION.SDK_INT >= 21) {

            switch (getColorPreference().getColorAsString(ColorUsage.ACCENT).toUpperCase()) {
                case "#F44336":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_red);
                    else
                        setTheme(R.style.pref_accent_dark_red);
                    break;

                case "#E91E63":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_pink);
                    else
                        setTheme(R.style.pref_accent_dark_pink);
                    break;

                case "#9C27B0":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_purple);
                    else
                        setTheme(R.style.pref_accent_dark_purple);
                    break;

                case "#673AB7":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_deep_purple);
                    else
                        setTheme(R.style.pref_accent_dark_deep_purple);
                    break;

                case "#3F51B5":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_indigo);
                    else
                        setTheme(R.style.pref_accent_dark_indigo);
                    break;

                case "#2196F3":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_blue);
                    break;

                case "#03A9F4":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_light_blue);
                    else
                        setTheme(R.style.pref_accent_dark_light_blue);
                    break;

                case "#00BCD4":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_cyan);
                    else
                        setTheme(R.style.pref_accent_dark_cyan);
                    break;

                case "#009688":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_teal);
                    else
                        setTheme(R.style.pref_accent_dark_teal);
                    break;

                case "#4CAF50":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_green);
                    break;

                case "#8BC34A":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_light_green);
                    else
                        setTheme(R.style.pref_accent_dark_light_green);
                    break;

                case "#FFC107":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_amber);
                    else
                        setTheme(R.style.pref_accent_dark_amber);
                    break;

                case "#FF9800":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_orange);
                    else
                        setTheme(R.style.pref_accent_dark_orange);
                    break;

                case "#FF5722":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_deep_orange);
                    else
                        setTheme(R.style.pref_accent_dark_deep_orange);
                    break;

                case "#795548":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_brown);
                    else
                        setTheme(R.style.pref_accent_dark_brown);
                    break;

                case "#212121":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_black);
                    else
                        setTheme(R.style.pref_accent_dark_black);
                    break;

                case "#607D8B":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_blue_grey);
                    else
                        setTheme(R.style.pref_accent_dark_blue_grey);
                    break;

                case "#004D40":
                    if (theme.equals(AppTheme.LIGHT))
                        setTheme(R.style.pref_accent_light_super_su);
                    else
                        setTheme(R.style.pref_accent_dark_super_su);
                    break;
            }
        } else {
            if (theme.equals(AppTheme.LIGHT)) {
                setTheme(R.style.appCompatLight);
            } else {
                setTheme(R.style.appCompatDark);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTheme();
    }

}
