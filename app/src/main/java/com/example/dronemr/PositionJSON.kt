package com.example.dronemr

import android.os.Bundle
import android.util.Log
import org.json.JSONObject

class PositionJSON {
    //JSON to send position to server
    private var team: String = "test"
    private var auth: String = "egtj-3jqa-z6fh-ete7-wrml"
    private var source: String = "3_AIR_DRONE-PATROLLER"
    private lateinit var position: JSONObject
    lateinit var positionJSON: JSONObject


    fun initialize() {
        positionJSON.put("team", team)
        //positionJSON.put("auth",auth)
        //positionJSON.put("source", source)
        //positionJSON.put("position", position)
        //positionJSON.put("altitude", NULL)
        //positionJSON.put("timestamp", System.currentTimeMillis())
    }


    fun updatePosition(latitude: Double, longitude: Double) {
        position.put("latitude", latitude)
        position.put("longitude", longitude)
        positionJSON.put("position", position)
    }

    fun updateAltitude(altitude: Double) {
        positionJSON.put("altitude", altitude)
    }

}