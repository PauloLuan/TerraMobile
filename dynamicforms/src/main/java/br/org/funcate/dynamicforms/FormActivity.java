/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.org.funcate.dynamicforms;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import br.org.funcate.dynamicforms.constraints.Constraints;
import br.org.funcate.dynamicforms.util.LibraryConstants;
import br.org.funcate.dynamicforms.util.Utilities;

import static br.org.funcate.dynamicforms.FormUtilities.TAG_IS_RENDER_LABEL;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_KEY;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_VALUE;

/**
 * The form activity.
 * <p/>
 * <p>This returns an array of {@link String} data that can be retrieved
 * through: {@link LibraryConstants#PREFS_KEY_FORM} and contain:</p>
 * <ul>
 * <li>longitude</li>
 * <li>latitude</li>
 * <li>elevation (or -1.0)</li>
 * <li>timestamp</li>
 * <li>a name for the form</li>
 * <li>the filled form data json</li>
 * </ul>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class FormActivity extends FragmentActivity {

    private double latitude = -9999.0;
    private double longitude = -9999.0;
    private double elevation = -9999.0;
    private String sectionName;
    private JSONObject sectionObject;
    private List<String> formNames4Section = new ArrayList<String>();
    private String sectionObjectString;
    private long noteId = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // make sure the orientation can't be changed once this activity started
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sectionName = extras.getString(LibraryConstants.PREFS_KEY_FORM_NAME);
            sectionObjectString = extras.getString(LibraryConstants.PREFS_KEY_FORM_JSON);
            latitude = extras.getDouble(LibraryConstants.LATITUDE);
            longitude = extras.getDouble(LibraryConstants.LONGITUDE);
            elevation = extras.getDouble(LibraryConstants.ELEVATION);
            noteId = extras.getLong(LibraryConstants.SELECTED_POINT_ID);
        }

        try {
            if (sectionObjectString == null) {
                sectionObject = TagsManager.getInstance(this).getSectionByName(sectionName);
                // copy the section object, which will be kept around along the activity
                sectionObjectString = sectionObject.toString();
            }

            sectionObject = new JSONObject(sectionObjectString);
            formNames4Section = TagsManager.getFormNames4Section(sectionObject);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        setContentView(R.layout.form);

    }

    /**
     * @return the list of titles.
     */
    public List<String> getFragmentTitles() {
        return formNames4Section;
    }

    public long getNoteId() {
        return noteId;
    }

    /**
     * @return the section names.
     */
    public String getSectionName() {
        return sectionName;
    }

    /**
     * @return the section object.
     */
    public JSONObject getSectionObject() {
        return sectionObject;
    }

    /**
     * @param sectionObject teh object to set.
     */
    public void setSectionObject(JSONObject sectionObject) {
        this.sectionObject = sectionObject;
    }

    /**
     * @return the lat
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return the lon
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Save action.
     *
     * @param view parent.
     */
    public void saveClicked(View view) {
        try {
            saveAction();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            Utilities.messageDialog(this, e.getLocalizedMessage(), null);
        }
    }

    /**
     * Cancel action.
     *
     * @param view parent.
     */
    public void cancelClicked(View view) {
        finish();
    }

    private void saveAction() throws Exception {
        // if in landscape mode store last inserted info, since that fragment has not been stored
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            FragmentDetail detailFragment = (FragmentDetail) getSupportFragmentManager().findFragmentById(R.id.detailFragment);
            if (detailFragment != null) {
                detailFragment.storeFormItems(false);
            }
        }

        // extract and check constraints
        List<String> availableFormNames = TagsManager.getFormNames4Section(sectionObject);
        String renderingLabel = null;
        for (String formName : availableFormNames) {
            JSONObject formObject = TagsManager.getForm4Name(formName, sectionObject);

            JSONArray formItemsArray = TagsManager.getFormItems(formObject);

            int length = formItemsArray.length();
            String value = null;
            for (int i = 0; i < length; i++) {
                JSONObject jsonObject = formItemsArray.getJSONObject(i);

                String key = "-";
                if (jsonObject.has(TAG_KEY))
                    key = jsonObject.getString(TAG_KEY).trim();

                if (jsonObject.has(TAG_VALUE)) {
                    value = jsonObject.getString(TAG_VALUE).trim();
                }
                if (jsonObject.has(TAG_IS_RENDER_LABEL)) {
                    String isRenderingLabelStr = jsonObject.getString(TAG_IS_RENDER_LABEL).trim();
                    boolean isRenderingLabel = Boolean.parseBoolean(isRenderingLabelStr);
                    if (isRenderingLabel)
                        renderingLabel = value;
                }

                // inject latitude
                if (key.equals(LibraryConstants.LATITUDE)) {
                    String latitudeString = String.valueOf(latitude);
                    value = latitudeString;
                    jsonObject.put(TAG_VALUE, latitudeString);
                }
                // inject longitude
                if (key.equals(LibraryConstants.LONGITUDE)) {
                    String longitudeString = String.valueOf(longitude);
                    value = longitudeString;
                    jsonObject.put(TAG_VALUE, longitudeString);
                }

                Constraints constraints = FormUtilities.handleConstraints(jsonObject, null);
                if (value == null || !constraints.isValid(value)) {
                    String constraintDescription = constraints.getDescription();
                    String validfieldMsg = getString(R.string.form_field_check);
                    String msg = Utilities.format(validfieldMsg, key, formName, constraintDescription);
                    Utilities.messageDialog(this, msg, null);
                    return;
                }
            }
        }

        // finally store data
        String sectionObjectString = sectionObject.toString();
        long timestamp = System.currentTimeMillis();

        if (renderingLabel == null) {
            renderingLabel = sectionName;
        }
        String[] formDataArray = {//
                String.valueOf(noteId), //
                String.valueOf(longitude), //
                String.valueOf(latitude), //
                String.valueOf(elevation), //
                String.valueOf(timestamp), //
                renderingLabel, //
                "POI", //
                sectionObjectString};
        Intent intent = getIntent();
        intent.putExtra(LibraryConstants.PREFS_KEY_FORM, formDataArray);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // force to exit through the exit button, in order to avoid losing info
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
