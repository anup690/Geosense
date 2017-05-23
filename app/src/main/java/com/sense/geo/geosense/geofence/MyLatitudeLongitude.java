package com.sense.geo.geosense.geofence;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sense.geo.geosense.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class MyLatitudeLongitude extends Fragment {


    private View rootView;
    private FragmentActivity mActivity;

    public MyLatitudeLongitude() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_my_latitude_longitude, container, false);

        mActivity = getActivity();
        init();
        return rootView;
    }

    private void init() {


    }

}
