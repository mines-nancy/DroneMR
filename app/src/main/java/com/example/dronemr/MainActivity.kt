package com.example.dronemr

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.dronemr.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.instrument.Altimeter
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.instrument.Compass
import com.parrot.drone.groundsdk.device.instrument.Gps
import com.parrot.drone.groundsdk.device.peripheral.ObstacleAvoidance
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.Directive.Speed
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf.RelativeMoveDirective
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.mavlink.ChangeSpeedCommand
import com.parrot.drone.groundsdk.mavlink.LandCommand
import com.parrot.drone.groundsdk.mavlink.MavlinkCommand
import com.parrot.drone.groundsdk.mavlink.MavlinkFiles
import com.parrot.drone.groundsdk.mavlink.NavigateToWaypointCommand
import com.parrot.drone.groundsdk.mavlink.ReturnToLaunchCommand
import com.parrot.drone.groundsdk.mavlink.SetRoiCommand
import com.parrot.drone.groundsdk.mavlink.TakeOffCommand
import com.parrot.drone.groundsdk.mavlink.standard.NavigateToWaypointCommand.Companion
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ConnectionSpec
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.json.JSONObject.NULL
import java.io.File
import java.io.IOException
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


const val TAG = "Sussy"

class MainActivity : AppCompatActivity(), GoogleMap.OnMapClickListener,
GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraIdleListener, OnMapReadyCallback, View.OnClickListener,
    GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerDragListener {

    private val droneIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.teal_200)
        BitmapHelper.vectorToBitmap(this, R.drawable.drone_icon, color)
    }

    private val waypointIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.teal_200)
        BitmapHelper.vectorToBitmap(this, R.drawable.waypoint_icon, color)
    }

    private val pointOfInterestIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(this, R.color.teal_200)
        BitmapHelper.vectorToBitmap(this, R.drawable.point_of_interest_24, color)
    }


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var groundSdk: GroundSdk

    /** get Drone **/
    var drone: Drone? = null

    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView

    /** Drone battery charge level text view. */
    private lateinit var droneBatteryTxt: TextView

    /** Reference to the current drone state. */
    private var droneStateRef: Ref<DeviceState>? = null

    /** Reference to the current drone battery info instrument. */
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null

    /** Reference to the current Gps info instrument */
    private var droneGPSInfoRef: Ref<Gps>? = null
    private lateinit var latitudeTxt: TextView
    private lateinit var longitudeTxt: TextView
    private lateinit var numberOfSatellites: TextView
    private var droneAltitudeInfoRef: Ref<Altimeter>? = null
    private var droneHeadingInfoRef: Ref<Compass>? = null
    private var droneMarker: Marker? = null
    private lateinit var mavlinkFile: File
    private lateinit var obstacleAvoidance: ObstacleAvoidance


    /** Reference to the current altitude info instrument */
    //private var droneAltitudeInfoRef : Ref<Altimeter>? = null
    private lateinit var altitudeTxt: TextView

    /** Reference to the current flightPlan info instrument */
    //private var flightPlanPilotingItfRef: Ref<FlightPlanPilotingItf>? = null

    /** Current remote control instance. */
    private var rc: RemoteControl? = null

    /** Reference to the current remote control state. */
    private var rcStateRef: Ref<DeviceState>? = null

    /** Reference to the current remote control battery info instrument. */
    private var rcBatteryInfoRef: Ref<BatteryInfo>? = null

    /** Remote state level text view. */
    private lateinit var rcStateTxt: TextView

    /** Remote battery charge level text view. */
    private lateinit var rcBatteryTxt: TextView

    /** Take off / land button. */
    private lateinit var takeOffLandBt: Button

    /** Reference to a current drone piloting interface. */
    private var pilotingItfRef: Ref<ManualCopterPilotingItf>? = null

    /**map */
    private lateinit var mMap: GoogleMap

    /**list of markers added */
    private val markers: MutableList<Marker> = arrayListOf()

    /**buttons */
    private lateinit var config: Button
    private lateinit var generate: Button
    private lateinit var start: Button
    private lateinit var stop: Button

    /**mission settings */
    private val missionList = mutableListOf<MavlinkCommand>()
    private val waypointList = mutableListOf<LatLng>()
    private var mAltitude: Double = 3.0
    private var mSpeed: Double = 3.0
    private var mFinishedAction: String = "autoland"
    private lateinit var missionControl : FlightPlanPilotingItf
    private var pointOfInterests = mutableListOf<LatLng>()

    /** Guided Piloting Interface */
    private lateinit var guidedPilotingItf: GuidedPilotingItf
    private var guidedPilotingItfRef: Ref<GuidedPilotingItf>? = null

    /**Http connection */
    private lateinit var client : OkHttpClient
    private lateinit var request : OkHttpRequest
    private lateinit var headingCommandRequest : OkHttpRequest
    private lateinit var moveCommandRequest : OkHttpRequest
    private lateinit var currentHeadingRequest : OkHttpRequest

    /**Camera option to follow drone */
    private var followingDrone : Boolean = false

    /**JSON to send position to server*/
    lateinit var droneInformation: JSONObject
    //message type
    private var messageType: String = "init"
    //drone type
    private var droneType: String = "leader"
    //identification
    private lateinit var identification: JSONObject
    private var team: String = "Mines Nancy"
    private var auth: String = "egtj-3jqa-z6fh-ete7-wrml"
    private var source: String = "3_AIR_DRONE-PATROLLER"
    private var color: String = "red"
    //position
    private lateinit var position: JSONObject

    //detection
    private lateinit var detection: JSONObject
    //command
    private lateinit var command: JSONObject
    var serverUrl: String = "https://webhook.site/1305b1e0-295a-4fe2-8f0e-d2a853fdb8a9"
    private var serverQuery: String = "/init"




    private lateinit var finalJSON: JSONObject

    /**handler to send data to server every X second */
    private var mHandler: Handler? = null

    /**Drawer menu button */
    private lateinit var galleryButton : Button
    private lateinit var cameraButton : Button

    /**Fragment */
    private lateinit var videoFragment: Fragment

    /**Leader or slave mode */
    private var isLeader = true
    private lateinit var leaderButton: MenuItem
    private lateinit var slaveButton: MenuItem

    /**server configuration */
    private val trustAllCertificates: Array<TrustManager> = arrayOf(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        }
    )

    private val trustAllCertificatesObject = object : X509TrustManager {
        override fun checkClientTrusted(
            chain: Array<java.security.cert.X509Certificate>?,
            authType: String?
        ) {
        }

        override fun checkServerTrusted(
            chain: Array<java.security.cert.X509Certificate>?,
            authType: String?
        ) {
        }

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    }


            // Initialize an SSLContext with the custom TrustManager
    val sslContext: SSLContext = SSLContext.getInstance("TLS")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //get the map
        val mapFragment = supportFragmentManager.findFragmentById(
            R.id.map_fragment
        ) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Construct a FusedLocationProviderClient.
        //fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        setSupportActionBar(binding.appBarMain.toolbar)

        //Button to follow the drone
        binding.appBarMain.fabFollow.setOnClickListener {
            if(followingDrone) {
                val myToast = Toast.makeText(this, "stopped following drone", Toast.LENGTH_SHORT)
                myToast.show()

            }
            else {
                val myToast = Toast.makeText(this, "started following drone", Toast.LENGTH_SHORT)
                myToast.show()
                val zoomLevel = 18.0.toFloat()
                val cu = CameraUpdateFactory.zoomTo(zoomLevel)
                mMap.animateCamera(cu)
            }

            followingDrone = !followingDrone
        }

        //Button to focus ondrone
        binding.appBarMain.fabFindUser.setOnClickListener {
            val drone = drone
            var latitude : Double = "0".toDouble()
            var longitude : Double = "0".toDouble()
            var pos = LatLng(latitude, longitude)
            if (drone == null) {
                val myToast = Toast.makeText(this, "no drone connected", Toast.LENGTH_SHORT)
                myToast.show()
            } else {
                droneGPSInfoRef = drone.getInstrument(Gps::class.java) { gps ->
                    gps?.lastKnownLocation().let { location ->
                        if (location != null) {
                            latitude = location.latitude
                            longitude = location.longitude
                            pos = LatLng(location.latitude, location.longitude)

                        } else {
                            val myToast =
                                Toast.makeText(this, "location not found", Toast.LENGTH_SHORT)
                            myToast.show()
                        }
                    }
                }
                if(latitude != "0".toDouble() && longitude != "0".toDouble()){
                    val zoomLevel = 18.0.toFloat()
                    val cu = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
                    mMap.animateCamera(cu)

                    val markerOptions = MarkerOptions()
                        .title("drone")
                        .position(pos)
                        .anchor(0.5F, 0.5F)
                        .icon(droneIcon)
                        .draggable(false)

                    this.runOnUiThread {
                        droneMarker?.remove()
                        if(checkGpsCoordination(latitude, longitude)){
                            droneMarker = mMap.addMarker(markerOptions)
                        }
                    }
                }
            }
        }

        binding.appBarMain.removeCheckpointsAndPoI.setOnClickListener {

            val markersSize = markers.lastIndex + 1
            val pointOfInterestsSize = pointOfInterests.lastIndex + 1
            markers.clear()
            pointOfInterests.clear()
            mMap.clear()
            waypointList.clear()

            if(markersSize > 0 || pointOfInterestsSize > 0) {


                Toast.makeText(this, "$markersSize marker(s) and $pointOfInterestsSize point(s) of interest(s) removed", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "no point to remove", Toast.LENGTH_SHORT).show()
            }


        }


        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_camera
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Get user interface instances.
        droneStateTxt = findViewById(R.id.droneStateTxt)
        droneBatteryTxt = findViewById(R.id.droneBatteryTxt)
        rcStateTxt = findViewById(R.id.rcStateTxt)
        rcBatteryTxt = findViewById(R.id.rcBatteryTxt)
        takeOffLandBt = findViewById(R.id.takeOffLandBt)
        takeOffLandBt.setOnClickListener { onTakeOffLandClick() }
        latitudeTxt = findViewById(R.id.labelDroneLat)
        longitudeTxt = findViewById(R.id.labelDroneLng)
        altitudeTxt = findViewById(R.id.altitudeTxt)
        numberOfSatellites = findViewById(R.id.numberOfSatellitesTxt)


        config = findViewById(R.id.config)
        generate = findViewById(R.id.generate)
        start = findViewById(R.id.start)
        stop = findViewById(R.id.stop)

        /**
        galleryButton = findViewById(R.id.nav_gallery)
        cameraButton = findViewById(R.id.nav_camera)
        galleryButton.isEnabled = false
        cameraButton.isEnabled = false
        */



        config.setOnClickListener(this)
        generate.setOnClickListener(this)
        start.setOnClickListener(this)
        stop.setOnClickListener(this)
        start.isEnabled = false
        stop.isEnabled = false



        // Initialize user interface default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(this)
        // All references taken are linked to the activity lifecycle and
        // automatically closed at its destruction.

        val sharedPref : SharedPreferences = getSharedPreferences("CoHoMaPrefs", Context.MODE_PRIVATE)
        team = sharedPref.getString("team", team).toString()
        auth = sharedPref.getString("auth", auth).toString()
        source = sharedPref.getString("source", source).toString()
        color = sharedPref.getString("color", color).toString()
        serverUrl = sharedPref.getString("serverUrl", serverUrl).toString()

        //create finalJSON
        finalJSON = JSONObject()

        //create droneInformation object
        droneInformation = JSONObject()

        //add message and drone type
        droneInformation.put("messageType", messageType)
        droneInformation.put("droneType", droneType)

        //add identification
        identification = JSONObject()
        identification.put("team", team)
        identification.put("auth",auth)
        identification.put("source", source)
        identification.put("color", color)
        droneInformation.put("identification", identification)

        //add position
        position = JSONObject()
        position.put("latitude", NULL)
        position.put("longitude", NULL)
        position.put("altitude", NULL)
        position.put("heading", NULL)
        droneInformation.put("position", position)

        //add droneInformation de finalJSON
        finalJSON.put("droneInformation", droneInformation)

        //add timestamp to finalJSON
        finalJSON.put("timestamp", System.currentTimeMillis())

        //initial request
        sslContext.init(null, trustAllCertificates, java.security.SecureRandom())
        client = OkHttpClient.Builder()
            .connectionSpecs(
                listOf<ConnectionSpec>(
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.COMPATIBLE_TLS,
                    ConnectionSpec.CLEARTEXT,
                )
            )
            .hostnameVerifier { _, _ -> true }
            .sslSocketFactory(sslContext.socketFactory, trustAllCertificatesObject)
            .build()

        request = OkHttpRequest(client, this)
        headingCommandRequest = OkHttpRequest(client, this)
        moveCommandRequest = OkHttpRequest(client, this)
        currentHeadingRequest = OkHttpRequest(client, this)

        //handler
        mHandler = Handler(mainLooper)

    }

    override fun onStart() {
        super.onStart()

        // Monitor the auto connection facility.
        groundSdk.getFacility(AutoConnection::class.java) {
            // Called when the auto connection facility is available and when it changes.

            it?.let {
                // Start auto connection.
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                // If the drone has changed.
                if (drone?.uid != it.drone?.uid) {

                    if (drone != null) {
                        // Stop monitoring the old drone.
                        stopDroneMonitors()

                        // Reset user interface drone part.
                        resetDroneUi()
                    }

                    // Monitor the new drone.
                    drone = it.drone
                    if (drone != null) {
                        startDroneMonitors()
                        /** Enable obstacle avoidance mode*/
                        //obstacleAvoidance = drone!!.getPeripheral(ObstacleAvoidance::class.java)!!
                        //obstacleAvoidance.preferredMode().value = ObstacleAvoidance.Mode.STANDARD
                        //guidedPilotingItf = drone!!.getPilotingItf(GuidedPilotingItf::class.java)!!

                    }
                }
                // If the remote control has changed.
                if (rc?.uid != it.remoteControl?.uid) {
                    if (rc != null) {
                        // Stop monitoring the old remote.
                        stopRcMonitors()

                        // Reset user interface Remote part.
                        resetRcUi()
                    }

                    // Monitor the new remote.
                    rc = it.remoteControl
                    if (rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()
        // Monitor drone battery charge level.
        monitorDroneBatteryChargeLevel()
        // Monitor piloting interface.
        monitorPilotingInterface()
        // Monitor drone GPS
        monitorDroneGPS()
        // Monitor drone Altitude
        monitorDroneAltitude()
        // Monitor drone Compass
        monitorDroneCompass()
        //send data
        startSending()

        //allow access to gallery and camera view
        /**
        galleryButton.isEnabled = true
        cameraButton.isEnabled = true
        */

    }

    /**
     * Stops drone monitors.
     */
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.

        droneStateRef?.close()
        droneStateRef = null

        droneBatteryInfoRef?.close()
        droneBatteryInfoRef = null

        pilotingItfRef?.close()
        pilotingItfRef = null

        droneGPSInfoRef?.close()
        droneGPSInfoRef = null

        droneAltitudeInfoRef?.close()
        droneAltitudeInfoRef = null

        droneHeadingInfoRef?.close()
        droneHeadingInfoRef = null

        /**
        galleryButton.isEnabled = false
        cameraButton.isEnabled = false
        */
    }

    /**
     * Monitors current drone battery charge level.
     */

    private fun monitorDroneBatteryChargeLevel() {
        // Monitor the battery info instrument.
        droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                droneBatteryTxt.text = "${it.charge} %"
            }
        }
    }



    /**
     * Monitor current drone state.
     */
    private fun monitorDroneState() {
        // Monitor current drone state.
        droneStateRef = drone?.getState {
            // Called at each drone state update.

            it?.let {
                // Update drone connection state view.
                droneStateTxt.text = it.connectionState.toString()

            }
        }
    }

    /**
     * Monitors current drone piloting interface.
     */
    private fun monitorPilotingInterface() {
        // Monitor a piloting interface.
        pilotingItfRef = drone?.getPilotingItf(ManualCopterPilotingItf::class.java) {
            // Called when the manual copter piloting Interface is available
            // and when it changes.

            // Disable the button if the piloting interface is not available.
            if (it == null) {
                takeOffLandBt.isEnabled = false
            } else {
                managePilotingItfState(it)
            }
        }

        /**flightPlanPilotingItfRef = drone?.getPilotingItf(FlightPlanPilotingItf::class.java) {
            if (it != null) {
                manageAutoPilotingItfState(it)

            }
        }*/
    }
    private fun monitorDroneGPS() {
        droneGPSInfoRef = drone?.getInstrument(Gps::class.java) { gps ->
            gps?.lastKnownLocation().let { location ->
                ("lng: " + location?.latitude.toString().chunked(16)[0]).also { latitudeTxt.text = it }
                ("lat: " + location?.longitude.toString().chunked(16)[0]).also { longitudeTxt.text = it }
//                Log.d(TAG, "Updated Location: ${it?.latitude}, ${it?.longitude}")
                if (location != null) {
                    updateDroneLocation(location.latitude, location.longitude)

                    if(followingDrone){
                        cameraUpdate(location.latitude, location.longitude, false)
                    }

                    //update data to send to server
                    position.put("latitude", location.latitude)
                    position.put("longitude", location.longitude)
                    val sharedPref : SharedPreferences = getSharedPreferences("CoHoMaPrefs", Context.MODE_PRIVATE)
                    val editor : SharedPreferences.Editor = sharedPref.edit()
                    editor.putFloat("latitude", location.latitude.toFloat())
                    editor.putFloat("longitude", location.longitude.toFloat())
                    editor.apply()
                    droneInformation.put("position", position)
                    finalJSON.put("droneInformation", droneInformation)
                }
            }
            if (gps != null) {
                numberOfSatellites.text = gps.satelliteCount.toString()
            }
        }
    }

    private fun monitorDroneCompass() {
        droneHeadingInfoRef = drone?.getInstrument(Compass::class.java) {compass ->
            compass?.heading.let { heading ->
                position.put("heading", heading)
                droneInformation.put("position", position)
                finalJSON.put("droneInformation", droneInformation)
            }
        }
    }


    private fun updateDroneLocation(
        latitude: Double,
        longitude: Double
    ) { // this will draw the aircraft as it moves
        if (latitude.isNaN() || longitude.isNaN()) {
            return
        }

        val pos = LatLng(latitude, longitude)
        // the following will draw the aircraft on the screen
        val markerOptions = MarkerOptions()
            .title("Drone")
            .position(pos)
            .anchor(0.5F, 0.5F)
            .icon(droneIcon)
            .draggable(false)


        this.runOnUiThread {
            droneMarker?.remove()
            if(checkGpsCoordination(latitude, longitude)){
            droneMarker = mMap.addMarker(markerOptions)
            }
        }
    }

    private fun checkGpsCoordination(
        latitude: Double,
        longitude: Double
    ): Boolean { // this will check if your gps coordinates are valid
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    private fun cameraUpdate(latitude: Double, longitude: Double, zoom : Boolean = false) {
        val pos = LatLng(latitude, longitude)
        if(!zoom) {
            val cu = CameraUpdateFactory.newLatLng(pos)
            mMap.animateCamera(cu)
        }
        else {
            val zoomLevel = 18.0.toFloat()
            val cu = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
            mMap.animateCamera(cu)
        }

    }


    private fun monitorDroneAltitude() {
        droneAltitudeInfoRef = drone?.getInstrument(Altimeter::class.java) { altimeter ->
            altimeter?.groundRelativeAltitude.let { altitude ->
                ("alt: " + altitude?.value.toString().chunked(16)[0]).also { altitudeTxt.text = it }
                if (altitude != null) {
                    position.put("altitude", altitude.value)
                    droneInformation.put("position", position)
                    finalJSON.put("droneInformation", droneInformation)
                }
            }
        }
    }

    private fun manageAutoPilotingItfState(itf: FlightPlanPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> {
                Log.d(TAG, "the state is unavailable")
            }

            Activable.State.IDLE -> {
//                val status = itf.activate(true)
//                Log.d(TAG, "activation status: $status - state is idle")
            }

            Activable.State.ACTIVE -> {
                Log.d(TAG, "state is active")
            }

        }
    }


    /**
     * Manage piloting interface state.
     *
     * @param itf the piloting interface
     */
    private fun managePilotingItfState(itf: ManualCopterPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> {
                // Piloting interface is unavailable.
                takeOffLandBt.isEnabled = false
            }

            Activable.State.IDLE -> {
                // Piloting interface is idle.
                takeOffLandBt.isEnabled = false

                // Activate the interface.
                itf.activate()
            }

            Activable.State.ACTIVE -> {
                // Piloting interface is active.

                when {

                    itf.canTakeOff() -> {
                        // Drone can take off.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = "Take Off"

                    }

                    itf.canLand() -> {
                        // Drone can land.
                        takeOffLandBt.isEnabled = true
                        takeOffLandBt.text = "Land"
                    }


                    else -> // Disable the button.
                        takeOffLandBt.isEnabled = false
                }
            }
        }
    }

    /**
     * Resets drone user interface part.
     */
    private fun resetDroneUi() {
        // Reset drone user interface views.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        droneBatteryTxt.text = ""
        takeOffLandBt.isEnabled = false
    }

    /**
     * Called on take off/land button click.
     */
    private fun onTakeOffLandClick() {

        // Get the piloting interface from its reference.
        pilotingItfRef?.get()?.let { itf ->
            // Do the action according to the interface capabilities
            if (itf.canTakeOff()) {
                // Take off
                itf.takeOff()
            } else if (itf.canLand()) {
                // Land
                itf.land()
            }
        }
    }

    /**
     * Resets remote user interface part.
     */
    private fun resetRcUi() {
        // Reset remote control user interface views.
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcBatteryTxt.text = ""
    }

    /**
     * Starts remote control monitors.
     */
    private fun startRcMonitors() {
        // Monitor remote state
        monitorRcState()

        // Monitor remote battery charge level
        monitorRcBatteryChargeLevel()
    }

    /**
     * Stops remote control monitors.
     */
    private fun stopRcMonitors() {
        // Close all references linked to the current remote to stop their monitoring.

        rcStateRef?.close()
        rcStateRef = null

        rcBatteryInfoRef?.close()
        rcBatteryInfoRef = null
    }

    /**
     * Monitor current remote control state.
     */
    private fun monitorRcState() {
        // Monitor current drone state.
        rcStateRef = rc?.getState {
            // Called at each remote state update.

            it?.let {
                // Update remote connection state view.
                rcStateTxt.text = it.connectionState.toString()
            }
        }
    }

    /**
     * Monitors current remote control battery charge level.
     */
    private fun monitorRcBatteryChargeLevel() {
        // Monitor the battery info instrument.
        rcBatteryInfoRef = rc?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                rcBatteryTxt.text = "${it.charge} %"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        leaderButton = menu.findItem(R.id.leader_settings)
        slaveButton = menu.findItem(R.id.slave_settings)


        if(isLeader) {
            val leaderTitle = SpannableString(leaderButton.title)
            leaderTitle.setSpan(ForegroundColorSpan(getColor(R.color.purple_500)), 0, leaderTitle.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            leaderButton.title = leaderTitle

            val slaveTitle = SpannableString(slaveButton.title)
            slaveTitle.setSpan(ForegroundColorSpan(getColor(R.color.purple_500_Translucent)), 0, slaveTitle.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            slaveButton.title = slaveTitle
        }

        else {
            val leaderTitle = SpannableString(leaderButton.title)
            leaderTitle.setSpan(ForegroundColorSpan(getColor(R.color.purple_500_Translucent)), 0, leaderTitle.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            leaderButton.title = leaderTitle

            val slaveTitle = SpannableString(slaveButton.title)
            slaveTitle.setSpan(ForegroundColorSpan(getColor(R.color.purple_500)), 0, slaveTitle.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            slaveButton.title = slaveTitle
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.leader_settings -> {
                // Action à effectuer lorsque l'élément "leader_settings" est sélectionné
                // Exemple : ouvrir une nouvelle activité, effectuer une opération, etc.
                invalidateOptionsMenu()
                isLeader = true
                droneType = "leader"
                droneInformation.put("droneType", droneType)
                finalJSON.put("droneInformation", droneInformation)
                generate.isEnabled = true
                return true
            }
            R.id.slave_settings -> {
                // Action à effectuer lorsque l'élément "slave_settings" est sélectionné
                // Exemple : ouvrir une nouvelle activité, effectuer une opération, etc.

                //generate.isEnabled = false
                start.isEnabled = false
                stop.isEnabled = false
                invalidateOptionsMenu()
                isLeader = false
                droneType = "slave"
                droneInformation.put("droneType", droneType)
                finalJSON.put("droneInformation", droneInformation)
                return true
            }
            // Autres éléments de menu (si nécessaire)
            // ...



            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }



    override fun onMapClick(p0: LatLng) {
        val marker = mMap.addMarker(
            MarkerOptions()
                .title((markers.lastIndex + 2).toString())
                .position(p0)
                .draggable(true)
                .icon(waypointIcon)
                .anchor(0.5F, 0.5F)

        )
        // Set place as the tag on the marker object so it can be referenced within
        // com.example.dronemr.MarkerInfoWindowAdapter
        if (marker != null) {
            marker.tag = "test"
            markers.add(marker)


        }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onMapLongClick(p0: LatLng) {
        val marker = mMap.addMarker(
            MarkerOptions()
                .title("Point Of Interest")
                .position(p0)
                .draggable(true)
                .icon(pointOfInterestIcon)
                .anchor(0.5F, 0.5F)
        )

        if(marker != null){
            marker.tag = "Point Of Interest"
            pointOfInterests.add(p0)
            Toast.makeText(this, "added point Of Interest", Toast.LENGTH_SHORT).show()

        }


        /**
        val size = markers.lastIndex + 1
        markers.clear()
        mMap.clear()
        waypointList.clear()
        if (size == 1) {
            val myToast = Toast.makeText(this, "marker removed", Toast.LENGTH_SHORT)
            myToast.show()
        }
        if (size > 1) {
            val myToast = Toast.makeText(this, "markers removed", Toast.LENGTH_SHORT)
            myToast.show()
        }
        */


    }

    override fun onMapReady(p0: GoogleMap) {

        print("getting ready")
        mMap = p0
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        //addClusteredMarkers(mMap)
        mMap.setOnMapClickListener(this)
        mMap.setOnMapLongClickListener(this)
        mMap.setOnInfoWindowClickListener(this)
        mMap.setOnMarkerDragListener(this)
        //mMap.setOnCameraIdleListener(this)
        val sharedPref : SharedPreferences = getSharedPreferences("CoHoMaPrefs", Context.MODE_PRIVATE)
        val lat = sharedPref.getFloat("latitude", "0".toFloat()).toDouble()
        val long = sharedPref.getFloat("longitude", "0".toFloat()).toDouble()
        val pos = LatLng(lat, long)
        val zoomLevel = 18.0.toFloat()
        val cu = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel)
        mMap.animateCamera(cu)


        // Turn on the My Location layer and the related control on the map.
        //updateLocationUI()

        // Get the current location of the device and set the position of the map.
        //getDeviceLocation()

    }

    /**
    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }
    */


    override fun onCameraIdle() {
        TODO("Not yet implemented")
    }

    fun sendMessageToServer(jsonMessage: String, url : String) {
        try {
            request.sendMessage(jsonMessage, url)
        } catch(e : IOException){
            println(e)
        }
    }

    private fun getDataFromServer(url: String) {
        try {
            headingCommandRequest.getData(url.plus("/headingCommand/").plus(source))
            moveCommandRequest.getData(url.plus("/moveCommand/").plus(source))

        } catch (e: IOException){
            println(e)
        }
    }

    private fun startSending() {
        // starting a coroutine
        MainScope().launch {
            var initSuccess = false
            var startedGetting = false
            while (drone != null) {
                if (!initSuccess) {
                    while (!initSuccess && drone != null) {
                        finalJSON.put("timestamp", System.currentTimeMillis())

                        sendMessageToServer(finalJSON.toString(), serverUrl.plus(serverQuery))
                        delay(2000L)
                        //initSuccess = true

                        /**
                        if(JSONObject(request.lastMessage)["success"] == "") {
                        initSuccess = true
                        }
                         */
                        if (request.lastMessage != "") {
                            /**
                            runOnUiThread {
                                Toast.makeText(
                                    baseContext,
                                    request.lastMessage,
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                            */

                            initSuccess = true
                        }
                    }
                }
                delay(500L)
                serverQuery = "/position"
                if(!startedGetting){
                    //Get data
                    startGetting()
                    startedGetting = true
                }
                messageType = "position"
                droneInformation.put("messageType", messageType)
                finalJSON.put("droneInformation", droneInformation)
                finalJSON.put("timestamp", System.currentTimeMillis())
                sendMessageToServer(finalJSON.toString(), serverUrl.plus(serverQuery).plus("/").plus(source))

            }
        }
    }

    @SuppressLint("Range")
    private fun startGetting() {
        GlobalScope.launch {
            while (drone !== null) {
                if(!isLeader) {
                    delay(500L)
                    getDataFromServer(serverUrl)
                    //sendLocationToServer(positionJSON.toString(), serverUrl)

                    runOnUiThread {
                        var rightDirective = 0
                        var forwardDirective = 0
                        if(moveCommandRequest.lastMessage != ""){
                            val moveCommand = JSONObject(moveCommandRequest.lastMessage)
                            rightDirective = moveCommand.getInt("rightDirective")
                            forwardDirective = moveCommand.getInt("forwardDirective")
                        }
                        if(abs(headingCommandRequest.lastMessage.toDouble()) > 40 || rightDirective != 0 || forwardDirective != 0) {

                            guidedPilotingItf =
                                drone?.getPilotingItf(GuidedPilotingItf::class.java)!!
                            if(guidedPilotingItf.state == Activable.State.IDLE) {
                                val speed = Speed(0.1, 0.1, 0.02)
                                val moveDirective = RelativeMoveDirective(
                                    forwardDirective.toDouble(),
                                    rightDirective.toDouble(),
                                    0.0,
                                    headingCommandRequest.lastMessage.toDouble()/2,
                                    speed
                                )
                                guidedPilotingItf.move(moveDirective)
                            }

                            //}
                        }

                        //guidedPilotingItf.move(moveDirective)
                    }

                }
            }
        }
    }


    private fun generateMission() {
        if (drone == null) {
            Toast.makeText(this, "No drone connected", Toast.LENGTH_SHORT).show()
        }
        else {
            if (isLeader) {
                Toast.makeText(this, "Generating mavlink mission file...", Toast.LENGTH_SHORT)
                    .show()

                val location = drone?.getInstrument(Gps::class.java)?.lastKnownLocation()

                waypointList.clear()
                missionList.clear()

                for (marker in markers) {
                    waypointList.add(marker.position)
                }
                Toast.makeText(this, waypointList.toString(), Toast.LENGTH_SHORT).show()


                missionList.add(
                    TakeOffCommand()

                )

                var currentPoIId: Int = -1

                if (pointOfInterests.size > 0 && waypointList.size > 0) {
                    val poiId = getClosestPoIId(waypointList[0])
                    currentPoIId = poiId
                    missionList.add(
                        SetRoiCommand(
                            pointOfInterests[poiId].latitude,
                            pointOfInterests[poiId].longitude,
                            0.0
                        )
                    )
                }

                missionList.add(
                    ChangeSpeedCommand(ChangeSpeedCommand.SpeedType.GROUND_SPEED, mSpeed)
                )
                waypointList.forEach { point ->
                    if (pointOfInterests.size > 0) {

                        val PoIId = getClosestPoIId(point)
                        if (currentPoIId != PoIId) {
                            currentPoIId = PoIId
                            missionList.add(
                                SetRoiCommand(
                                    pointOfInterests[currentPoIId].latitude,
                                    pointOfInterests[currentPoIId].longitude,
                                    0.0
                                )
                            )
                        }
                    }

                    missionList.add(
                        NavigateToWaypointCommand(
                            point.latitude,
                            point.longitude,
                            mAltitude,
                            0.0,
                            Companion.DEFAULT_HOLD_TIME,
                            Companion.DEFAULT_ACCEPTANCE_RADIUS
                        )
                    )
                }


                if (location != null) {
                    when (mFinishedAction) {
                        "gohome" -> {


                            missionList.add(ReturnToLaunchCommand())
                            missionList.add(LandCommand())
                        }

                        "autoland" -> {

                            missionList.add(LandCommand())
                        }

                        "none" -> {

                        }

                        "firstwaypoint" -> {

                            missionList.add(missionList[2])
                            missionList.add(LandCommand())
                        }
                    }
                }
                val folder = getExternalFilesDir("flight_plan")
                mavlinkFile = File(folder, "flight_plan.txt")

                MavlinkFiles.generate(
                    mavlinkFile,
                    missionList,
                )

                missionControl = drone?.getPilotingItf(FlightPlanPilotingItf::class.java)!!
                GlobalScope.launch {
                    sendMissionToDrone()
                }

                start.isEnabled = true
                stop.isEnabled = true

            } else {
                Toast.makeText(this, request.lastMessage, Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun getClosestPoIId(point: LatLng) : Int {
        var minIndex = 0
        var minDist = distance(point, pointOfInterests[0])
        pointOfInterests.forEachIndexed { index, PoI ->
            val dist =  distance(point, PoI)
            if(dist < minDist){
                minIndex = index
                minDist = dist
            }
        }
        return minIndex

    }

    private fun distance(point1: LatLng, point2: LatLng) : Double {
        return (sqrt((point1.latitude - point2.latitude).pow(2) + (point1.longitude - point2.longitude).pow(2)))
    }


    private fun getYaw(point1 : LatLng, point2: LatLng) : Double {
        var yaw = 0.0


        yaw = kotlin.math.atan2(sqrt((point1.latitude-point2.latitude).pow(2)),
            sqrt((point1.longitude-point2.longitude).pow(2)))
        yaw = yaw*180/kotlin.math.PI

        val myToast = Toast.makeText(this, "yaw $yaw", Toast.LENGTH_SHORT)
        myToast.show()

        return yaw
    }


    private fun sendMissionToDrone() {
            missionControl.clearRecoveryInfo()
            missionControl.uploadFlightPlan(mavlinkFile, "new_plan")

            //Toast.makeText(this, "uploading mission...", Toast.LENGTH_SHORT).show()

            //Toast.makeText(this, missionControl.latestUploadState.toString(), Toast.LENGTH_SHORT).show()

        }


    private fun startMission() {
        /**val missionControl = drone?.getPilotingItf(FlightPlanPilotingItf::class.java)
        Toast.makeText(this, "trying sending", Toast.LENGTH_SHORT).show()
        if (missionControl != null) {
            //clear old data
            missionControl.clearRecoveryInfo()
            Log.d(TAG, mavlinkFile.absolutePath)

            //upload new flight plan
            missionControl.uploadFlightPlan(mavlinkFile, "new_plan")

            //tell behavior if disconnected
            missionControl.returnHomeOnDisconnect.isEnabled = true
            Log.d(TAG, "latest mission: " +  missionControl.latestMissionItemExecuted.toString() )
            Toast.makeText(this,"uploadstate" +missionControl.latestUploadState , Toast.LENGTH_SHORT).show()

            Log.d(
                TAG,
                "it.returnHomeOnDisconnect: " + missionControl.returnHomeOnDisconnect.isEnabled.toString()
            )
            while(missionControl.latestUploadState == FlightPlanPilotingItf.UploadState.UPLOADING) {
                Thread.sleep(1000)
                Toast.makeText(this,"uploading mission", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(this,missionControl.latestUploadState.toString(), Toast.LENGTH_SHORT).show()
        */

        if (missionControl != null){
            missionControl.returnHomeOnDisconnect.isEnabled = true

            if (missionControl.state == Activable.State.ACTIVE) {
                Toast.makeText(this,"mission already started" , Toast.LENGTH_SHORT).show()
            }
            if (missionControl.state == Activable.State.IDLE) {

                val missionStarted = missionControl.activate(FlightPlanPilotingItf.Interpreter.STANDARD,true)
                if (missionStarted) {
                    Toast.makeText(this, "mission started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "mission couldn't be started", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun stopMission() {
        val missionControl = drone?.getPilotingItf(FlightPlanPilotingItf::class.java)
        val isStopped = missionControl?.stop()
        if (isStopped == true) {
            Toast.makeText(this, "mission has been stopped", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "mission couldn't be stopped", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "mission is stopped = $isStopped")
    }

    private fun showLeaderSettingsDialog() {

        val wayPointSettings = layoutInflater.inflate(R.layout.dialog_leadersettings, null) as LinearLayout

        val altitudeEditText = wayPointSettings.findViewById<View>(R.id.altitude) as EditText
        altitudeEditText.setText(mAltitude.toString())

        val speedEditText = wayPointSettings.findViewById<View>(R.id.speed) as EditText
        speedEditText.setText(mSpeed.toString())


        val actionAfterFinishedRG = wayPointSettings.findViewById<View>(R.id.actionAfterFinished) as RadioGroup
        actionAfterFinishedRG.setOnCheckedChangeListener { _, checkedId -> // set the action after finishing the mission
            Log.d(TAG, "Select finish action")

            when (checkedId) {
                R.id.finishNone -> {
                    mFinishedAction = "none"
                }
                R.id.finishGoHome -> {
                    mFinishedAction = "gohome"
                }
                R.id.finishAutoLanding -> {
                    mFinishedAction = "autoland"
                }
                R.id.finishToFirst -> {
                    mFinishedAction = "firstwaypoint"
                }
            }
        }


        val nameEditText = wayPointSettings.findViewById<View>(R.id.team) as EditText
        nameEditText.setText(team)

        val authEditText = wayPointSettings.findViewById<View>(R.id.auth) as EditText
        authEditText.setText(auth)

        val sourceEditText = wayPointSettings.findViewById<View>(R.id.source) as EditText
        sourceEditText.setText(source)

        val serverUrlEditText = wayPointSettings.findViewById<View>(R.id.serverUrl) as EditText
        serverUrlEditText.setText(serverUrl)

        AlertDialog.Builder(this) // creates the dialog
            .setTitle("Leader Configuration")
            .setView(wayPointSettings)
            .setPositiveButton("Finish") { _, _ ->
                mAltitude = altitudeEditText.text.toString().toDouble()
                mSpeed = speedEditText.text.toString().toDouble()
                team = nameEditText.text.toString()
                auth = authEditText.text.toString()
                source = sourceEditText.text.toString()
                serverUrl = serverUrlEditText.text.toString()
                Log.e(TAG, "altitude $mAltitude")
                Log.e(TAG, "speed $mSpeed")
                Log.e(TAG, "mFinishedAction $mFinishedAction")
                identification.put("team", team)
                identification.put("auth",auth)
                identification.put("source", source)
                identification.put("color", color)
                generate.isEnabled = true
                val sharedPref : SharedPreferences = getSharedPreferences("CoHoMaPrefs", Context.MODE_PRIVATE)
                val editor : SharedPreferences.Editor = sharedPref.edit()
                editor.putString("team", team)
                editor.putString("auth", auth)
                editor.putString("source", source)
                editor.putString("serverUrl", serverUrl)
                editor.apply()

                Toast.makeText(this, "Finished configuring mission settings", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .create()
            .show()
    }

    private fun showSlaveSettingsDialog() {
        val slaveSettings = layoutInflater.inflate(R.layout.dialog_slavesettings, null) as LinearLayout

        val altitudeEditText = slaveSettings.findViewById<View>(R.id.altitude) as EditText
        altitudeEditText.setText(mAltitude.toString())

        val nameEditText = slaveSettings.findViewById<View>(R.id.team) as EditText
        nameEditText.setText(team)

        val authEditText = slaveSettings.findViewById<View>(R.id.auth) as EditText
        authEditText.setText(auth)

        val sourceEditText = slaveSettings.findViewById<View>(R.id.source) as EditText
        sourceEditText.setText(source)

        val colorEditText = slaveSettings.findViewById<View>(R.id.color) as EditText
        colorEditText.setText(color)

        val serverUrlEditText = slaveSettings.findViewById<View>(R.id.serverUrl) as EditText
        serverUrlEditText.setText(serverUrl)

        AlertDialog.Builder(this) // creates the dialog
            .setTitle("Slave Configuration")
            .setView(slaveSettings)
            .setPositiveButton("Finish") { _, _ ->
                mAltitude = altitudeEditText.text.toString().toDouble()
                team = nameEditText.text.toString()
                auth = authEditText.text.toString()
                source = sourceEditText.text.toString()
                color = colorEditText.text.toString()
                serverUrl = serverUrlEditText.text.toString()
                Log.e(TAG, "altitude $mAltitude")
                Log.e(TAG, "speed $mSpeed")
                Log.e(TAG, "mFinishedAction $mFinishedAction")

                val sharedPref : SharedPreferences = getSharedPreferences("CoHoMaPrefs", Context.MODE_PRIVATE)
                val editor : SharedPreferences.Editor = sharedPref.edit()
                editor.putString("team", team)
                editor.putString("auth", auth)
                editor.putString("source", source)
                editor.putString("color", color)
                editor.putString("serverUrl", serverUrl)
                editor.apply()

                Toast.makeText(this, "Finished configuring mission settings", Toast.LENGTH_SHORT).show()

                /** idk if useful yet
                positionJSON.put("team", team)
                positionJSON.put("auth",auth)
                positionJSON.put("source", source)
                positionJSON.put("position", position)
                generate.isEnabled = true
                */

            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .create()
            .show()
    }



    override fun onClick(p0: View?) {
        when (p0?.id) {
            /**R.id.locate -> { // will draw the drone and move camera to the position of the drone on the map
                val location =  drone?.getInstrument(Gps::class.java)?.lastKnownLocation()
                latitudeTxt.text = location?.latitude.toString()
                longitudeTxt.text = location?.longitude.toString()
                Log.d(TAG, "Location on Btn Click: ${location?.latitude}, ${location?.longitude}")
                if (location != null) {
                    updateDroneLocation(location.latitude, location.longitude)
                    cameraUpdacameraUpdate(location.latitude, location.longitude)
                }
            }
            R.id.add -> { // this will toggle the adding of the waypoints
                enableDisableAdd()
            }
            R.id.clear -> { // clear the waypoints on the map
                runOnUiThread {
                    mMap.clear()
                    clearMission()
                }
            } */

            R.id.config -> { // this will show the settings
                if(isLeader){
                    showLeaderSettingsDialog()
                }
                else {
                    showSlaveSettingsDialog()
                }
            }

            R.id.generate -> { // this will upload the mission to the drone so that it can execute it
                generateMission()
            }
            R.id.start -> { // this will let the drone start navigating to the waypoints
                startMission()
            }
            R.id.stop -> { // this will immediately stop the waypoint mission
                stopMission()
            } else -> {}
        }
    }


    override fun onInfoWindowClick(p0: Marker) {

        if (p0.title == "Point Of interest"){
            pointOfInterests.remove(p0.position)
            p0.remove()
        }
        else if (p0.title == "Drone"){

        }
        else {
            val markerIndex = markers.indexOf(p0)

            // Mettre à jour les marqueurs suivants
            if (markerIndex != -1 && markerIndex < markers.size - 1) {
                for (i in markerIndex + 1 until markers.size) {
                    val nextMarker = markers[i]
                    // Mettre à jour le titre ou d'autres propriétés du marqueur suivant
                    // Par exemple, vous pouvez incrémenter le titre
                    nextMarker.title = (nextMarker.title?.toInt()?.minus(1)).toString()
                }
            }

            p0.remove()
            markers.remove(p0)
        }


    }

    override fun onMarkerDrag(p0: Marker) {
        //do nothing
    }

    override fun onMarkerDragEnd(marker: Marker) {
        // Mettre à jour la position du marqueur dans la liste markers
        val markerIndex = markers.indexOf(marker)
        if (markerIndex != -1) {
            markers[markerIndex] = marker
        }
    }
    override fun onMarkerDragStart(p0: Marker) {
        //do nothing
    }

}





