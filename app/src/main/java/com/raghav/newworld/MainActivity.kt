package com.raghav.newworld

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.raghav.newworld.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var listCharacters = ArrayList<DisneyCharacter>()
    private var location: Location? = null
    private var oldLocation: Location? = null

    private val ACCESS_LOCATION = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadCharacter()
        checkPermission()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.action_settings -> {
                return true
            }
            R.id.set_hybrid -> {
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                return true
            }
            R.id.set_normal -> {
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                return true
            }
            R.id.set_satellite -> {
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                return true
            }
            R.id.set_terrain -> {
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                return true
            }
            R.id.set_angle_45 -> {
                location?.let {
                    val latlong = LatLng(it.latitude, it.longitude)
                    val cameraPosition = CameraPosition.Builder()
                        .target(latlong)
                        .zoom(10f)
                        .tilt(45f)
                        .build()
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
                return true
            }
            R.id.set_default -> {
                location?.let {
                    val latlong = LatLng(it.latitude, it.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlong, 10f))
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), ACCESS_LOCATION)
                return
            }
        }
        getUserLocation()
    }

    private fun getUserLocation() {
        Toast.makeText(this, "Location access allowed!", Toast.LENGTH_LONG).show()
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val myLocationListener = MyLocationListener()
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3f, myLocationListener)
        MyThread().start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ACCESS_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getUserLocation()
                } else {
                    Toast.makeText(this, "Location access not allowed!", Toast.LENGTH_LONG).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun resizeMarkerIcon(id: Int, width: Int, height: Int): BitmapDescriptor {
        val bitmapDrawable = BitmapFactory.decodeResource(resources, id)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmapDrawable, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap)
    }

    inner class MyLocationListener : LocationListener {
        init {
            location = Location("Start").apply {
                longitude = 0.0
                latitude = 0.0
            }
        }

        override fun onLocationChanged(p0: Location) {
            location = p0
        }

        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            // Not yet implemented
        }

        override fun onProviderEnabled(p0: String) {
            // Not yet implemented
        }

        override fun onProviderDisabled(p0: String) {
            // Not yet implemented
        }
    }

    inner class MyThread : Thread() {
        init {
            oldLocation = Location("Start").apply {
                latitude = 0.0
                longitude = 0.0
            }
        }

        override fun run() {
            while (true) {
                try {
                    location?.let {
                        if (oldLocation?.distanceTo(it) == 0f) {
                            return@let // No movement
                        }
                        oldLocation = location
                        runOnUiThread {
                            mMap.clear()
                            val profileMarker = resizeMarkerIcon(R.drawable.profile, 200, 200)
                            val currentLocation = LatLng(it.latitude, it.longitude)
                            mMap.addMarker(MarkerOptions().position(currentLocation).title("Me").snippet("Here is my location").icon(profileMarker))
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10f))

                            for (character in listCharacters) {
                                val characterMarker = resizeMarkerIcon(character.image!!, 200, 200)
                                val characterLocation = LatLng(character.location!!.latitude, character.location!!.longitude)
                                mMap.addMarker(MarkerOptions().position(characterLocation).title(character.name).snippet(character.description).icon(characterMarker))

                                if (it.distanceTo(character.location!!) < 600) {
                                    Toast.makeText(applicationContext, "You met ${character.name}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    Thread.sleep(1000)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun loadCharacter() {
        listCharacters.apply {
            add(DisneyCharacter(R.drawable.americandragon, "American Dragon", "A Dragon Boy", 37.9883, -115.0221))
            add(DisneyCharacter(R.drawable.ariel, "Ariel", "Sea Princess", 37.6608, -114.5243))
            add(DisneyCharacter(R.drawable.bambi, "Bambi", "Jungle Prince", 38.0099, -115.1220))
            add(DisneyCharacter(R.drawable.pluto, "Pluto", "Good dog", 37.9263, -115.1390))
            add(DisneyCharacter(R.drawable.goofy, "Goofy", "Best friend", 37.8877, -115.1490))
            add(DisneyCharacter(R.drawable.donald, "Donald", "Nice friend", 37.8447, -115.0580))
        }
    }
}
