package com.example.firemapkotlin

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.firemapkotlin.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val REQUEST_LOCATION: Int = 1
    lateinit var locationList: ArrayList<User>
    lateinit var currentLocation: Location
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var database: DatabaseReference
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        locationList = arrayListOf()

        database = Firebase.database("https://fir-map-4660d-default-rtdb.firebaseio.com/").reference

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun animateZoomInCamera(latLng: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }

    private val reqSetting = LocationRequest.create().apply {
        fastestInterval = 10000
        interval = 10000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        smallestDisplacement = 1.0f
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val currentCity = LatLng(18.444030074081, 73.86919634662)
        val zoomLevel = 1.0f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentCity, zoomLevel))
        mMap.addMarker(MarkerOptions().position(currentCity).title("Marker in Katraj"))
        setMapLongClickListener(mMap)
        setOnPoiClick(mMap)

        database.child("Locations").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (locationSnapShot in snapshot.children) {
                        val location = locationSnapShot.getValue(User::class.java)
                        locationList.add(location!!)
                        println(location)

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        fetchLocation()
    }


    private fun setMapLongClickListener(map: GoogleMap) {
        map.setOnMapLongClickListener {
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Lng: %2$.5f",
                it.latitude,
                it.longitude
            )

            map.addMarker(MarkerOptions().position(it).title("Marker here!").snippet(snippet))

            val reference =
                database.child("Locations").push().setValue(User(it.latitude, it.longitude))
            /*

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (location in snapshot.children) {
                            println(snapshot)
*//*
                            locationList = arrayListOf()
                            locationList.add(latLng!!)*//*
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })*/
        }
    }

    private fun setOnPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener {
            val poiMarker = map.addMarker(MarkerOptions().position(it.latLng).title(it.name))
            poiMarker?.showInfoWindow()
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
        } else {
            val task: Task<Location> = fusedLocationProviderClient.lastLocation

            task.addOnSuccessListener {

                if (it != null) {
                    currentLocation = it
                    animateZoomInCamera(
                        LatLng(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                    )
                } else {

                    val REQUEST_CHECK_STATE = 12300 // any suitable ID
                    val builder = LocationSettingsRequest.Builder()
                        .addLocationRequest(reqSetting)

                    val client = LocationServices.getSettingsClient(this)
                    client.checkLocationSettings(builder.build()).addOnCompleteListener { task ->
                        try {
                            val state: LocationSettingsStates = task.result!!.locationSettingsStates
                            Log.d("salam", task.result!!.toString())
                            Log.e(
                                "LOG", "LocationSettings: \n" +
                                        " GPS present: ${state.isGpsPresent} \n" +
                                        " GPS usable: ${state.isGpsUsable} \n" +
                                        " Location present: " +
                                        "${state.isLocationPresent} \n" +
                                        " Location usable: " +
                                        "${state.isLocationUsable} \n" +
                                        " Network Location present: " +
                                        "${state.isNetworkLocationPresent} \n" +
                                        " Network Location usable: " +
                                        "${state.isNetworkLocationUsable} \n"
                            )
                        } catch (e: RuntimeExecutionException) {
                            Log.d("salam", "hei")
                            if (e.cause is ResolvableApiException)
                                (e.cause as ResolvableApiException).startResolutionForResult(
                                    this,
                                    REQUEST_CHECK_STATE
                                )
                        }
                    }

                    val locationUpdates = object : LocationCallback() {
                        override fun onLocationResult(lr: LocationResult) {
                            Log.e("salam", lr.toString())
                            Log.e("salam", "Newest Location: " + lr.locations.last())
                            // do something with the new location...
                            animateZoomInCamera(
                                LatLng(
                                    lr.locations.last().latitude,
                                    lr.locations.last().longitude
                                )
                            )

                            val snippet = String.format(
                                Locale.getDefault(),
                                "Lat: %1$.5f, Lng: %2$.5f",
                                lr.locations.last().latitude,
                                lr.locations.last().longitude
                            )

                            mMap.addMarker(
                                MarkerOptions().position(
                                    LatLng(
                                        lr.locations.last().latitude,
                                        lr.locations.last().longitude
                                    )
                                ).title("Marker here!").snippet(snippet)
                            )
                        }
                    }

                    fusedLocationProviderClient.requestLocationUpdates(
                        reqSetting,
                        locationUpdates,
                        null /* Looper */
                    )

                    fusedLocationProviderClient.removeLocationUpdates(locationUpdates)
                }
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.fetchLocation()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required to locate you!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}