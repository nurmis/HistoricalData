package com.aware.ui.esms;

/**
 * Created by denzilferreira on 21/02/16.
 */

import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builder class for ESM questions. Any new ESM type needs to extend this class.
 */
public class ESM_Question extends DialogFragment {

    public JSONObject esm = new JSONObject();

    public int _id;

    public static final String esm_type = "esm_type";
    public static final String esm_title = "esm_title";
    public static final String esm_instructions = "esm_instructions";
    public static final String esm_submit = "esm_submit";
    public static final String esm_expiration_threshold = "esm_expiration_threshold";
    public static final String esm_trigger = "esm_trigger";
    public static final String esm_flows = "esm_flows";
    public static final String flow_user_answer = "user_answer";
    public static final String flow_next_esm = "next_esm";

    protected ESM_Question setID(int id) {
        _id = id;
        return this;
    }

    public int getID() {
        return _id;
    }

    public int getType() throws JSONException {
        if (!this.esm.has(esm_type)) return -1;
        return this.esm.getInt(esm_type);
    }

    /**
     * Set ESM type ID. AWARE includes:
     * com.aware.ESM#TYPE_ESM_TEXT
     * com.aware.ESM#TYPE_ESM_RADIO
     * com.aware.ESM#TYPE_ESM_CHECKBOX
     * com.aware.ESM#TYPE_ESM_LIKERT
     * com.aware.ESM#TYPE_ESM_QUICK_ANSWERS
     * com.aware.ESM#TYPE_ESM_SCALE
     *
     * @param type
     * @return
     * @throws JSONException
     */
    protected ESM_Question setType(int type) throws JSONException {
        this.esm.put(esm_type, type);
        return this;
    }

    public String getTitle() throws JSONException {
        if (!this.esm.has(esm_title)) {
            this.esm.put(esm_title, "");
        }
        return this.esm.getString(esm_title);
    }

    /**
     * Set ESM title, limited to about 50 characters due to phone's screen size when in portrait.
     *
     * @param title
     * @return
     * @throws JSONException
     */
    public ESM_Question setTitle(String title) throws JSONException {
        this.esm.put(esm_title, title);
        return this;
    }

    public String getInstructions() throws JSONException {
        if (!this.esm.has(esm_instructions)) {
            this.esm.put(esm_instructions, "");
        }
        return this.esm.getString(esm_instructions);
    }

    public ESM_Question setInstructions(String instructions) throws JSONException {
        this.esm.put(esm_instructions, instructions);
        return this;
    }

    public String getSubmitButton() throws JSONException {
        if (!this.esm.has(esm_submit)) {
            this.esm.put(esm_submit, "OK");
        }
        return this.esm.getString(esm_submit);
    }

    public ESM_Question setSubmitButton(String submit) throws JSONException {
        this.esm.put(esm_submit, submit);
        return this;
    }

    public int getExpirationThreshold() throws JSONException {
        if (!this.esm.has(esm_expiration_threshold)) {
            this.esm.put(esm_expiration_threshold, 0);
        }
        return this.esm.getInt(esm_expiration_threshold);
    }

    /**
     * For how long this question is visible waiting for the users' interaction
     *
     * @param expiration_threshold
     * @return
     * @throws JSONException
     */
    public ESM_Question setExpirationThreshold(int expiration_threshold) throws JSONException {
        this.esm.put(esm_expiration_threshold, expiration_threshold);
        return this;
    }

    public String getTrigger() throws JSONException {
        if (!this.esm.has(esm_trigger)) {
            this.esm.put(esm_trigger, "");
        }
        return this.esm.getString(esm_trigger);
    }

    /**
     * A label for what triggered this ESM
     *
     * @param trigger
     * @return
     * @throws JSONException
     */
    public ESM_Question setTrigger(String trigger) throws JSONException {
        this.esm.put(esm_trigger, trigger);
        return this;
    }

    /**
     * Get questionnaire flow.
     *
     * @return
     * @throws JSONException
     */
    public JSONArray getFlows() throws JSONException {
        if (!this.esm.has(esm_flows)) {
            this.esm.put(esm_flows, new JSONArray());
        }
        return this.esm.getJSONArray(esm_flows);
    }

    /**
     * Set questionnaire flow.
     *
     * @param esm_flow
     * @return
     * @throws JSONException
     */
    public ESM_Question setFlows(JSONArray esm_flow) throws JSONException {
        this.esm.put(esm_flows, esm_flow);
        return this;
    }

    /**
     * Add a flow condition to this ESM
     *
     * @param user_answer
     * @param nextEsm
     * @return
     * @throws JSONException
     */
    public ESM_Question addFlow(String user_answer, int nextEsm) throws JSONException {
        JSONArray flows = getFlows();
        flows.put(new JSONObject()
                .put(flow_user_answer, user_answer)
                .put(flow_next_esm, nextEsm));

        this.setFlows(flows);
        return this;
    }

    /**
     * Given user's answer, what's the next esm ID?
     *
     * @param user_answer
     * @return
     * @throws JSONException
     */
    public int getFlow(String user_answer) throws JSONException {
        JSONArray flows = getFlows();
        for (int i = 0; i < flows.length(); i++) {
            JSONObject flow = flows.getJSONObject(i);
            if (flow.getString(flow_user_answer).equals(user_answer))
                return flow.getInt(flow_next_esm);
        }
        return -1;
    }

    /**
     * Remove a flow condition from this ESM based on user's answer
     *
     * @param user_answer
     * @return
     * @throws JSONException
     */
    public ESM_Question removeFlow(String user_answer) throws JSONException {
        JSONArray flows = getFlows();
        JSONArray new_flows = new JSONArray();
        for (int i = 0; i < flows.length(); i++) {
            JSONObject flow = flows.getJSONObject(i);
            if (flow.getString(flow_user_answer).equals(user_answer)) continue;
            new_flows.put(flow);
        }
        this.setFlows(new_flows);
        return this;
    }

    public JSONObject build() throws JSONException {
        JSONObject esm = new JSONObject();
        esm.put("esm", this.esm);
        return esm;
    }

    /**
     * Rebuild ESM_Question object from database JSON
     *
     * @param esm
     * @return
     * @throws JSONException
     */
    public ESM_Question rebuild(JSONObject esm) throws JSONException {
        this.esm = esm;
        return this;
    }

    /**
     * COMMON CODE TO HANDLE ESM INTERACTIONS
     */
    public Dialog esm_dialog = null;
    public ESMExpireMonitor expire_monitor = null;

    /**
     * Extended on sub-classes
     *
     * @param savedInstanceState
     * @return
     */
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            if (getExpirationThreshold() > 0) {
                expire_monitor = new ESMExpireMonitor(System.currentTimeMillis(), getExpirationThreshold(), getID());
                expire_monitor.execute();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return esm_dialog;
    }

    /**
     * Checks on the background if the current visible dialog has expired or not. If it did, removes dialog and updates the status to expired.
     *
     * @author denzil
     */
    public class ESMExpireMonitor extends AsyncTask<Void, Void, Void> {
        private long display_timestamp = 0;
        private int expires_in_seconds = 0;
        private int esm_id = 0;

        public ESMExpireMonitor(long display_timestamp, int expires_in_seconds, int esm_id) {
            this.display_timestamp = display_timestamp;
            this.expires_in_seconds = expires_in_seconds;
            this.esm_id = esm_id;
        }

        @Override
        protected Void doInBackground(Void... params) {
            while ((System.currentTimeMillis() - display_timestamp) / 1000 <= expires_in_seconds) {
                if (isCancelled()) {
                    Cursor esm = getActivity().getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, ESM_Provider.ESM_Data._ID + "=" + esm_id, null, null);
                    if (esm != null && esm.moveToFirst()) {
                        int status = esm.getInt(esm.getColumnIndex(ESM_Provider.ESM_Data.STATUS));
                        switch (status) {
                            case ESM.STATUS_ANSWERED:
                                if (Aware.DEBUG) Log.d(Aware.TAG, "ESM has been answered!");
                                break;
                            case ESM.STATUS_DISMISSED:
                                if (Aware.DEBUG) Log.d(Aware.TAG, "ESM has been dismissed!");
                                break;
                        }
                    }
                    if (esm != null && !esm.isClosed()) esm.close();
                    return null;
                }
            }

            if (Aware.DEBUG) Log.d(Aware.TAG, "ESM has expired!");

            ContentValues rowData = new ContentValues();
            rowData.put(ESM_Provider.ESM_Data.ANSWER_TIMESTAMP, System.currentTimeMillis());
            rowData.put(ESM_Provider.ESM_Data.STATUS, ESM.STATUS_EXPIRED);
            getActivity().getContentResolver().update(ESM_Provider.ESM_Data.CONTENT_URI, rowData, ESM_Provider.ESM_Data._ID + "=" + esm_id, null);

            Intent expired = new Intent(ESM.ACTION_AWARE_ESM_EXPIRED);
            getActivity().sendBroadcast(expired);

            esm_dialog.dismiss();

            return null;
        }
    }

    /**
     * When dismissing one ESM by pressing cancel, the rest of the queue gets dismissed
     */
    private void dismissESM() {

        ContentValues rowData = new ContentValues();
        rowData.put(ESM_Provider.ESM_Data.ANSWER_TIMESTAMP, System.currentTimeMillis());
        rowData.put(ESM_Provider.ESM_Data.STATUS, ESM.STATUS_DISMISSED);
        getActivity().getContentResolver().update(ESM_Provider.ESM_Data.CONTENT_URI, rowData, ESM_Provider.ESM_Data._ID + "=" + getID(), null);

        Cursor esm = getActivity().getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, ESM_Provider.ESM_Data.STATUS + " IN (" + ESM.STATUS_NEW + "," + ESM.STATUS_VISIBLE + ")", null, null);
        if (esm != null && esm.moveToFirst()) {
            do {
                rowData = new ContentValues();
                rowData.put(ESM_Provider.ESM_Data.ANSWER_TIMESTAMP, System.currentTimeMillis());
                rowData.put(ESM_Provider.ESM_Data.STATUS, ESM.STATUS_DISMISSED);
                getActivity().getContentResolver().update(ESM_Provider.ESM_Data.CONTENT_URI, rowData, null, null);
            } while (esm.moveToNext());
        }
        if (esm != null && !esm.isClosed()) esm.close();

        Intent answer = new Intent(ESM.ACTION_AWARE_ESM_DISMISSED);
        getActivity().sendBroadcast(answer);

        if (esm_dialog != null) esm_dialog.dismiss();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        try {
            if (getExpirationThreshold() > 0 && expire_monitor != null) expire_monitor.cancel(true);
            dismissESM();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        try {
            if (getExpirationThreshold() > 0 && expire_monitor != null) expire_monitor.cancel(true);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if (ESM.isESMVisible(getActivity().getApplicationContext())) {
            if (Aware.DEBUG)
                Log.d(Aware.TAG, "ESM was visible but not answered, go back to notification bar");

            //Revert to NEW state
            ContentValues rowData = new ContentValues();
            rowData.put(ESM_Provider.ESM_Data.ANSWER_TIMESTAMP, 0);
            rowData.put(ESM_Provider.ESM_Data.STATUS, ESM.STATUS_NEW);
            getActivity().getContentResolver().update(ESM_Provider.ESM_Data.CONTENT_URI, rowData, ESM_Provider.ESM_Data._ID + "=" + getID(), null);

            //Update notification
            ESM.notifyESM(getActivity().getApplicationContext());

            esm_dialog.dismiss();
            getActivity().finish();
        }
    }
}
