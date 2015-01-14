package com.mannykary.mapmymeetups;

/**
 * Created by manny on 11/14/14.
 */
public class Place {
    public String description;
    public String placeId;
    public String title;
    public String subtitle;

    public Place(String d, String id) {
        description = d;
        placeId = id;
        splitDesc(description);
    }

    private void splitDesc(String d) {
        int i = d.indexOf(',');
        title = d.substring(0, i);
        subtitle = d.substring(i+2);
    }
}
