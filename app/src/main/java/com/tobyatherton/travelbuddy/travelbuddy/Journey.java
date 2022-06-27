package com.tobyatherton.travelbuddy.travelbuddy;


import android.location.Location;

public class Journey {

    Location pStartLocation = null;
    Location pEndLocation = null;
    String pCountry = "";

    public Journey(Location startLocation, Location endLocation, String country, String topSpeed,int totalSteps,int highestAltitude, int lowestAltitude) {
        pStartLocation = startLocation;
        pEndLocation = endLocation;
        pCountry = country;
    }

}
