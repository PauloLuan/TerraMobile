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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import br.org.funcate.dynamicforms.constraints.Constraints;
import br.org.funcate.dynamicforms.views.GMapView;
import br.org.funcate.dynamicforms.views.GView;

import static br.org.funcate.dynamicforms.FormUtilities.TAG_KEY;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_LABEL;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_READONLY;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_SIZE;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_TYPE;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_URL;
import static br.org.funcate.dynamicforms.FormUtilities.TAG_VALUE;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_BOOLEAN;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_CONNECTEDSTRINGCOMBO;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_DATE;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_DOUBLE;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_INTEGER;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_LABEL;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_LABELWITHLINE;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_PICTURES;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_SKETCH;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_STRING;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_STRINGAREA;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_STRINGCOMBO;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_STRINGMULTIPLECHOICE;
import static br.org.funcate.dynamicforms.FormUtilities.TYPE_TIME;

/**
 * The fragment detail view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FragmentDetail extends Fragment {

    private HashMap<String, GView> key2WidgetMap = new HashMap<String, GView>();
    private SparseArray<GView> requestCodes2WidgetMap = new SparseArray<GView>();
    private HashMap<String, Constraints> key2ConstraintsMap = new HashMap<String, Constraints>();
    private List<String> keyList = new ArrayList<String>();
    private String selectedFormName;
    private JSONObject sectionObject;
    private long noteId = -1;
    private double longitude;
    private double latitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.details, container, false);
        LinearLayout mainView = (LinearLayout) view.findViewById(R.id.form_linear);
        try {
            FragmentActivity activity = getActivity();
            if (selectedFormName == null || sectionObject == null) {
                FragmentList listFragment = (FragmentList) getFragmentManager().findFragmentById(R.id.listFragment);
                if (listFragment != null) {
                    selectedFormName = listFragment.getSelectedItemName();
                    sectionObject = listFragment.getSectionObject();
                    noteId = listFragment.getNoteId();
                    longitude = listFragment.getLongitude();
                    latitude = listFragment.getLatitude();
                } else {
                    if (activity instanceof FragmentDetailActivity) {
                        // case of portrait mode
                        FragmentDetailActivity fragmentDetailActivity = (FragmentDetailActivity) activity;
                        selectedFormName = fragmentDetailActivity.getFormName();
                        sectionObject = fragmentDetailActivity.getSectionObject();
                        noteId = fragmentDetailActivity.getNoteId();
                        longitude = fragmentDetailActivity.getLongitude();
                        latitude = fragmentDetailActivity.getLatitude();
                    }
                }
            }
            if (selectedFormName != null) {
                JSONObject formObject = TagsManager.getForm4Name(selectedFormName, sectionObject);

                key2WidgetMap.clear();
                requestCodes2WidgetMap.clear();
                int requestCode = 666;
                keyList.clear();
                key2ConstraintsMap.clear();

                JSONArray formItemsArray = TagsManager.getFormItems(formObject);

                int length = formItemsArray.length();
                for (int i = 0; i < length; i++) {
                    JSONObject jsonObject = formItemsArray.getJSONObject(i);

                    String key = "-"; //$NON-NLS-1$
                    if (jsonObject.has(TAG_KEY))
                        key = jsonObject.getString(TAG_KEY).trim();

                    String label = key;
                    if (jsonObject.has(TAG_LABEL))
                        label = jsonObject.getString(TAG_LABEL).trim();

                    String value = ""; //$NON-NLS-1$
                    if (jsonObject.has(TAG_VALUE)) {
                        value = jsonObject.getString(TAG_VALUE).trim();
                    }
                    String type = FormUtilities.TYPE_STRING;
                    if (jsonObject.has(TAG_TYPE)) {
                        type = jsonObject.getString(TAG_TYPE).trim();
                    }

                    boolean readonly = false;
                    if (jsonObject.has(TAG_READONLY)) {
                        String readonlyStr = jsonObject.getString(TAG_READONLY).trim();
                        readonly = Boolean.parseBoolean(readonlyStr);
                    }

                    Constraints constraints = new Constraints();
                    FormUtilities.handleConstraints(jsonObject, constraints);
                    key2ConstraintsMap.put(key, constraints);
                    String constraintDescription = constraints.getDescription();

                    GView addedView = null;
                    if (type.equals(TYPE_STRING)) {
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 0, 0, constraintDescription,
                                readonly);
                    } else if (type.equals(TYPE_STRINGAREA)) {
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 0, 7, constraintDescription,
                                readonly);
                    } else if (type.equals(TYPE_DOUBLE)) {
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 1, 0, constraintDescription,
                                readonly);
                    } else if (type.equals(TYPE_INTEGER)) {
                        addedView = FormUtilities.addEditText(activity, mainView, label, value, 4, 0, constraintDescription,
                                readonly);
                    } else if (type.equals(TYPE_DATE)) {
                        addedView = FormUtilities.addDateView(FragmentDetail.this, mainView, label, value, constraintDescription,
                                readonly);
                    } else if (type.equals(TYPE_TIME)) {
                        addedView = FormUtilities.addTimeView(FragmentDetail.this, mainView, label, value, constraintDescription,
                                readonly);
                    } else if (type.equals(TYPE_LABEL)) {
                        String size = "20"; //$NON-NLS-1$
                        if (jsonObject.has(TAG_SIZE))
                            size = jsonObject.getString(TAG_SIZE);
                        String url = null;
                        if (jsonObject.has(TAG_URL))
                            url = jsonObject.getString(TAG_URL);
                        addedView = FormUtilities.addTextView(activity, mainView, value, size, false, url);
                    } else if (type.equals(TYPE_LABELWITHLINE)) {
                        String size = "20"; //$NON-NLS-1$
                        if (jsonObject.has(TAG_SIZE))
                            size = jsonObject.getString(TAG_SIZE);
                        String url = null;
                        if (jsonObject.has(TAG_URL))
                            url = jsonObject.getString(TAG_URL);
                        addedView = FormUtilities.addTextView(activity, mainView, value, size, true, url);
                    } else if (type.equals(TYPE_BOOLEAN)) {
                        addedView = FormUtilities.addBooleanView(activity, mainView, label, value, constraintDescription, readonly);
                    } else if (type.equals(TYPE_STRINGCOMBO)) {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addComboView(activity, mainView, label, value, itemsArray, constraintDescription);
                    } else if (type.equals(TYPE_CONNECTEDSTRINGCOMBO)) {
                        LinkedHashMap<String, List<String>> valuesMap = TagsManager.extractComboValuesMap(jsonObject);
                        addedView = FormUtilities.addConnectedComboView(activity, mainView, label, value, valuesMap,
                                constraintDescription);
                    } else if (type.equals(TYPE_STRINGMULTIPLECHOICE)) {
                        JSONArray comboItems = TagsManager.getComboItems(jsonObject);
                        String[] itemsArray = TagsManager.comboItems2StringArray(comboItems);
                        addedView = FormUtilities.addMultiSelectionView(activity, mainView, label, value, itemsArray,
                                constraintDescription);
                    } else if (type.equals(TYPE_PICTURES)) {
                        addedView = FormUtilities.addPictureView(noteId, this, requestCode, mainView, label, value, constraintDescription);
                    } else if (type.equals(TYPE_SKETCH)) {
                        addedView = FormUtilities.addSketchView(noteId, this, requestCode, mainView, label, value, constraintDescription);
                    }else{
                        Toast.makeText(getActivity().getApplicationContext(), "Type non implemented yet: " + type, Toast.LENGTH_LONG).show();
                    }
/*                    } else if (type.equals(TYPE_MAP)) {
                        if (value.length() <= 0) {
                            // need to read image
                            File tempDir = ResourcesManager.getInstance(activity).getTempDir();
                            File tmpImage = new File(tempDir, LibraryConstants.TMPPNGIMAGENAME);
                            if (tmpImage.exists()) {
                                byte[][] imageAndThumbnailFromPath = ImageUtilities.getImageAndThumbnailFromPath(tmpImage.getAbsolutePath(), 1);
                                Date date = new Date();
                                String mapImageName = ImageUtilities.getMapImageName(date);

                                IImagesDbHelper imageHelper = DefaultHelperClasses.getDefaulfImageHelper();
                                long imageId = imageHelper.addImage(longitude, latitude, -1.0, -1.0, date.getTime(), mapImageName, imageAndThumbnailFromPath[0], imageAndThumbnailFromPath[1], noteId);
                                value = "" + imageId;
                            }
                        }
                        addedView = FormUtilities.addMapView(activity, mainView, label, value, constraintDescription);
                    } else if (type.equals(TYPE_NFCUID)) {
                        addedView = new GNfcUidView(this, null, requestCode, mainView, label, value, constraintDescription);
                    } else {
                        GPLog.addLogEntry(this, null, null, "Type non implemented yet: " + type);
                    }*/
                    key2WidgetMap.put(key, addedView);
                    keyList.add(key);
                    requestCodes2WidgetMap.put(requestCode, addedView);
                    requestCode++;
                }

            }
        } catch (Exception e) {
            //GPLog.error(context, null, e);
            Toast.makeText(getActivity().getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return view;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            GView gView = requestCodes2WidgetMap.get(requestCode);
            if (gView != null) {
                gView.setOnActivityResult(data);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        for (int i = 0; i < requestCodes2WidgetMap.size(); i++) {
            int key = requestCodes2WidgetMap.keyAt(i);
            // get the object by the key.
            GView view = requestCodes2WidgetMap.get(key);
            if (view instanceof GMapView) {
                try {
                    view.refresh(((GMapView) view).getContext());
                } catch (Exception e) {
                    //GPLog.error(this, null, e);
                    Toast.makeText(getActivity().getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * @return the section object.
     */
    public JSONObject getSectionObject() {
        return sectionObject;
    }

    /**
     * Setter for the form.
     *
     * @param selectedItemName form name.
     * @param sectionObject    section object.
     */
    public void setForm(String selectedItemName, JSONObject sectionObject) {
        this.selectedFormName = selectedItemName;
        this.sectionObject = sectionObject;
    }

    /**
     * Store the form items the widgets.
     *
     * @param doConstraintsCheck if <code>true</code>, a check on all constraints is performed.
     * @return <code>null</code>, if everything was saved properly, the key of the items
     * that didn't pass the constraint check.
     * @throws Exception if something goes wrong.
     */
    public String storeFormItems(boolean doConstraintsCheck) throws Exception {
        if (selectedFormName == null) {
            return null;
        }
        JSONObject form4Name = TagsManager.getForm4Name(selectedFormName, sectionObject);
        JSONArray formItems = TagsManager.getFormItems(form4Name);

        // update the items
        for (String key : keyList) {
            Constraints constraints = key2ConstraintsMap.get(key);

            GView view = key2WidgetMap.get(key);
            if (view != null) {
                String text = view.getValue();
                if (doConstraintsCheck && !constraints.isValid(text)) {
                    return key;
                }

                try {
                    if (text != null)
                        FormUtilities.update(formItems, key, text);
                } catch (JSONException e) {
                    throw new Exception(e.getMessage());
                }
            }
        }

        FragmentActivity activity = getActivity();
        if (activity instanceof FragmentDetailActivity) {
            // case of portrait mode
            FragmentDetailActivity fragmentDetailActivity = (FragmentDetailActivity) activity;
            FormUtilities.updateExtras(formItems, fragmentDetailActivity.getLatitude(), fragmentDetailActivity.getLongitude());
        } else {
            throw new RuntimeException("Fragmentlist not available"); //$NON-NLS-1$
        }

        return null;
    }

}