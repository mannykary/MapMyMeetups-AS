# Map My Meetups

The Map My Meetups app uses the Google Maps and Meetup.com APIs to display all
open events occurring within a user-specified area.

Check out the latest version of my app [here](https://github.com/mannykary/MapMyMeetups-AS/releases). Note that it is still in alpha pre-release, and so there will still be bugs in my app. If you find any issues, feel free to open up an issue [here](https://github.com/mannykary/MapMyMeetups-AS/issues/new).

![Screenshot of Map My Meetups](https://raw.githubusercontent.com/mannykary/MapMyMeetups-AS/master/MapMyMeetups_screenshot_01.png)
![Screenshot of Map My Meetups](https://raw.githubusercontent.com/mannykary/MapMyMeetups-AS/master/MapMyMeetups_screenshot_02.png)

Upon lauching the app, the map will zoom to your current location. You can then
long-tap anywhere on the map to view all events (up to 50) that are occurring within
24 hours and in a 5 mile radius from the tapped location. Red markers will appear 
on the map (you may need to zoom out in order to see all the markers). Tapping a marker
will display an Info Window showing the date/time of the event, and a short 
description of the event. Tapping on the Info Window will launch the application
of your choice to view detailed information about the event (either the Meetup 
app if installed on your device, or a browser to launch the event page on the
Meetup.com website.

The user can also search for meetup events satisfying their criteria, as seen in the screenshot below.

![Screenshot of Map My Meetups](https://raw.githubusercontent.com/mannykary/MapMyMeetups-AS/master/MapMyMeetups_screenshot_03.png)

If you wish to fork/clone the source, add the following file to your res/values/ 
folder (name it apikeys.xml). Replace YOUR_API_KEY with your actual API keys.

```
<?xml version="1.0" encoding="utf-8"?>
<resources>
    
    <string name="GoogleMapsKey">YOUR_API_KEY</string>
    
</resources>
```

Also add the following Java class (APIKeys.java) to app/src/main/java. 
Replace YOUR_API_KEY with your actual API keys.

```
package com.mannykary.mapmymeetups;

public class APIKeys {

    public static final String GOOGLE_PLACES = "YOUR_API_KEY";
    public static final String MEETUP = "YOUR_API_KEY";
}
```
