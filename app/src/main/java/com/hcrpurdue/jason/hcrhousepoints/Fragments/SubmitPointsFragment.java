/**
 * SubmitPointsFragment displays the form where a user can submit a point. Contains label for
 * point type, and description. Also date and time input and description input
 */

package com.hcrpurdue.jason.hcrhousepoints.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.hcrpurdue.jason.hcrhousepoints.Activities.NavigationActivity;
import com.hcrpurdue.jason.hcrhousepoints.Models.PointType;
import com.hcrpurdue.jason.hcrhousepoints.Models.ResponseCodeMessage;
import com.hcrpurdue.jason.hcrhousepoints.R;
import com.hcrpurdue.jason.hcrhousepoints.Utils.CacheManager;
import com.hcrpurdue.jason.hcrhousepoints.Utils.UtilityInterfaces.ListenerCallbackInterface;
import com.hcrpurdue.jason.hcrhousepoints.Utils.UtilityInterfaces.CacheManagementInterface;
import com.tsongkha.spinnerdatepicker.SpinnerDatePickerDialogBuilder;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

public class SubmitPointsFragment extends Fragment implements ListenerCallbackInterface {
    private static final int START_MONTH = Calendar.AUGUST;
    private static final int START_DAY = 1;

    static private CacheManager cacheManager;
    private Context context;
    private AppCompatActivity activity;
    private ProgressBar progressBar;
    private PointType pointType;
    private EditText descriptionEditText;
    private TextView pointTypeTextView;
    private TextView pointTypeDescriptionTextView;
    private Button submitPointButton;

    private Calendar calendar;
    private DatePicker dp;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cacheManager = CacheManager.getInstance(getContext());
        calendar = new GregorianCalendar();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_submit_point, container, false);
        retrieveBundleData();
        cacheManager.getCachedData();

        dp = view.findViewById(R.id.date_button);

        final Calendar minDateCal = Calendar.getInstance();
        int currentYear = minDateCal.get(Calendar.YEAR);
        int currentMonth = minDateCal.get(Calendar.MONTH);
        int currentDay = minDateCal.get(Calendar.DAY_OF_MONTH);

        // set min selectable date
        minDateCal.set(Calendar.YEAR, currentYear - 1);
        minDateCal.set(Calendar.MONTH, START_MONTH);
        minDateCal.set(Calendar.DAY_OF_MONTH, START_DAY);
        dp.setMinDate(minDateCal.getTimeInMillis());

        // set max selectable date
        final Calendar maxDateCal = Calendar.getInstance();
        maxDateCal.set(Calendar.YEAR, currentYear);
        maxDateCal.set(Calendar.MONTH, currentMonth);
        maxDateCal.set(Calendar.DAY_OF_MONTH, currentDay);
        maxDateCal.set(Calendar.HOUR, 11);
        maxDateCal.set(Calendar.MINUTE, 59);
        maxDateCal.set(Calendar.SECOND, 59);
        maxDateCal.set(Calendar.MILLISECOND, 59);
        maxDateCal.set(Calendar.AM_PM, 1);
        dp.setMaxDate(maxDateCal.getTimeInMillis());

        pointTypeDescriptionTextView = view.findViewById(R.id.submit_point_type_description_text_view);
        descriptionEditText = view.findViewById(R.id.description_edit_text);
        pointTypeTextView = view.findViewById(R.id.submit_point_type_text_view);

        pointTypeTextView.setText(pointType.getName());
        pointTypeDescriptionTextView.setText(pointType.getPointDescription());
        submitPointButton = view.findViewById(R.id.submit_point_button);
        submitPointButton.setOnClickListener(view1 -> submitPoint());
        return view;
    } //onCreateView

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (AppCompatActivity) getActivity();
        progressBar = Objects.requireNonNull(activity).findViewById(R.id.navigationProgressBar);
    }

    public void submitPoint() {
        calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth());
        InputMethodManager inputManager = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager != null && getActivity().getCurrentFocus() != null) // Avoids null pointer exceptions
            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        if (TextUtils.isEmpty(descriptionEditText.getText().toString().trim()))
            Toast.makeText(context, "Description is Required", Toast.LENGTH_SHORT).show();
        else if (descriptionEditText.length() > 250) {
            Toast.makeText(context, "Description cannot be more than 250 characters", Toast.LENGTH_SHORT).show();
        } else {
            progressBar.setVisibility(View.VISIBLE);

            cacheManager.submitPoints(descriptionEditText.getText().toString(), calendar.getTime(), pointType, new CacheManagementInterface() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.INVISIBLE);
                    ((NavigationActivity) activity).animateSuccess();
                    getFragmentManager().popBackStackImmediate();
                }

                @Override
                public void onHttpError(ResponseCodeMessage responseCodeMessage) {
                    progressBar.setVisibility(View.INVISIBLE);
                    System.out.println("API ERROR in submitting point: "+responseCodeMessage.getMessage());
                    Toast.makeText(context, "Sorry. There was an error submitting your point. Please Try again.", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(Exception e, Context context) {
                    progressBar.setVisibility(View.INVISIBLE);
                    System.out.println(" ERROR in submitting point: "+e.getLocalizedMessage());
                    Toast.makeText(context, "Sorry. There was an error submitting your point. Please Try again.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Attempts to retrieve a PointType model from the Bundle. Call this when initializing the view.
     * To put values in the correct place, when you build a fragmentTransatction, put a Bundle into the fragment with fragment.setArguments(Bundle);
     */
    private void retrieveBundleData() {
        Bundle bundle = getArguments();
        if (bundle != null) {
            pointType = (PointType) bundle.getSerializable("POINTTYPE");
        }
        if (pointType == null) {
            pointType = new PointType(0, "Fake", "Fake Description", true, 1000, true, 3);
        }
    }

}
