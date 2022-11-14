module com.udacity.catpoint.security {
    requires miglayout;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires transitive java.prefs;
    requires transitive com.udacity.catpoint.image;
    opens com.udacity.catpoint.security.data to com.google.gson;
}