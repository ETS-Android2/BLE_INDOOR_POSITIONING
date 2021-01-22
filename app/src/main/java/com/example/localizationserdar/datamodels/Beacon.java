package com.example.localizationserdar.datamodels;

import com.google.firebase.firestore.PropertyName;

import java.util.HashMap;
import java.util.Map;

import static com.example.localizationserdar.utils.Constants.BEACON_ID;
import static com.example.localizationserdar.utils.Constants.BEACON_NAME;

public class Beacon {
    @PropertyName(BEACON_NAME)
    public String beaconName;
    @PropertyName(BEACON_ID)
    public String beaconId;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(BEACON_NAME, beaconName);
        map.put(BEACON_ID, beaconId);

        return map;
    }
}
