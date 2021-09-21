package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.example.myapplication.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.example.myapplication.PermissionUtils.isPermissionGranted
import com.example.myapplication.PermissionUtils.requestPermission
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.gson.Gson


class MapsActivity : AppCompatActivity(), OnMyLocationButtonClickListener,
    OnMapReadyCallback, OnRequestPermissionsResultCallback, OnMapLongClickListener,
    OnMarkerClickListener {

    private var permissionDenied = false
    private var locationAccess = false
    private var radius = 100f
    private lateinit var map: GoogleMap
    private lateinit var circle: Circle
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTask: Task<Location>
    private lateinit var locationRequest: LocationRequest
    private lateinit var sharedPreferences: SharedPreferences

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                if (checkPositionIn(circle, location)) {
                    circle.fillColor = Color.argb(65, 0, 255, 0)
                    circle.strokeColor = Color.rgb(0, 255, 0)
                    locationAccess = true
                } else {
                    circle.fillColor = Color.argb(65, 255, 0, 0)
                    circle.strokeColor = Color.rgb(255, 0, 0)
                    locationAccess = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.interval = 4000
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        sharedPreferences = getSharedPreferences("sharedPrefs",MODE_PRIVATE)
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun checkSettingsAndStartLocationUpdates() {
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest).build()
        val client = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask = client.checkLocationSettings(request)
        locationSettingsResponseTask.addOnSuccessListener {
            startLocationUpdates()
        }
        locationSettingsResponseTask.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this, 1001)
                } catch (ex: SendIntentException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
            return
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap ?: return
        map.setOnMyLocationButtonClickListener(this)
        enableMyLocation()
        map.setOnMapLongClickListener(this)
        map.setOnMarkerClickListener(this)
        val cameraState = loadCameraState()
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraState))
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCameraState()
    }

    private fun saveCameraState(){
        val cameraPosition = map.cameraPosition
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val jsonCameraPosition = Gson().toJson(cameraPosition)
        editor.putString("CameraPosition",jsonCameraPosition)
        editor.apply()
    }

    private fun loadCameraState(): CameraPosition{
        val jsonCameraPosition = sharedPreferences.getString("CameraPosition","")
        return Gson().fromJson(jsonCameraPosition, CameraPosition::class.java)
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        )
            return
        locationTask = fusedLocationClient.lastLocation
        /*locationTask.addOnSuccessListener { location ->
            if (location != null) {
                Log.d("Location", location.toString())
            } else {
                Log.d("Null", "onSuccess: Location was null...")
            }
        }

        locationTask.addOnFailureListener { e ->
            Log.e(
                "Fail",
                "onFailure: " + e.localizedMessage
            )
        }*/
    }

    override fun onMapLongClick(latLng: LatLng) {
        stopLocationUpdates()
        map.clear()
        addMarker(latLng)
        addCircle(latLng, radius)
        checkSettingsAndStartLocationUpdates()
    }

    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
            map.isMyLocationEnabled = true
        } else {
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation()
            getLastLocation()
        } else {
            permissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (locationAccess) {
            val intent = Intent(this@MapsActivity, EntityActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(applicationContext, "Access denied!", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun addMarker(latLng: LatLng) {
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title("DataBase")
            .snippet("Radius: 100m.")
        map.addMarker(markerOptions)
    }

    private fun addCircle(latLng: LatLng, radius: Float) {
        val circleOptions = CircleOptions()
        circleOptions.center(latLng)
        circleOptions.radius(radius.toDouble())
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0))
        circleOptions.fillColor(Color.argb(64, 255, 0, 0))
        circleOptions.strokeWidth(4f)
        circle = map.addCircle(circleOptions)
    }

    private fun checkPositionIn(circle: Circle, location: Location): Boolean {
        val distance = FloatArray(2)
        Location.distanceBetween(
            location.latitude, location.longitude,
            circle.center.latitude, circle.center.longitude, distance
        )
        return distance[0] < circle.radius
    }
}
