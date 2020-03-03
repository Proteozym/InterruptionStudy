package de.lmu.js.interruptionesm


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    val MY_PERMISSIONS_REQUEST_READ_PHONE_STATE: Int = 0;
    val MY_PERMISSIONS_REQUEST_SMS: Int = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, 0, 0
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED) {

// We do not have this permission. Let’s ask the user
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)!= PackageManager.PERMISSION_GRANTED) {

// We do not have this permission. Let’s ask the user
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.RECEIVE_SMS),
                MY_PERMISSIONS_REQUEST_SMS);
        }
        startService(Intent(this@MainActivity, InterruptionStudyService::class.java))

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                //setContentView(R.layout.activity_timeline)
                val myIntent = Intent(this, TimelineActivity::class.java)
                startActivityForResult(myIntent, 0)
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        //LocalBroadcastManager.getInstance(this).registerReceiver(activityReceiver!!, IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY))
    }

    override fun onPause() {
       super.onPause()
        Log.d("Ö switch", "Is this switch?")
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(activityReceiver!!)
    }

    override fun onStop() {
        super.onStop();

    }

    override fun onDestroy() {
        super.onDestroy()

    }


}


