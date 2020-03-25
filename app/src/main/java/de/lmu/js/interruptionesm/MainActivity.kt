package de.lmu.js.interruptionesm


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aware.Applications
import com.aware.Aware
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.protobuf.LazyStringArrayList


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    val MY_PERMISSIONS_REQUEST: Int = 1;
    var permList = mutableListOf<String>()
    var viewPermList = mutableListOf<permissionView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Initialize Firebase Auth
        var auth = FirebaseAuth.getInstance()

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        //updateUI(currentUser)

        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Ö", "signInAnonymously:success")
                    val user = auth.currentUser
                    //updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Ö", "signInAnonymously:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    //updateUI(null)
                }

                // ...
            }

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, 0, 0
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED) {
            Log.d("Ö", "Phone State---------")
// We do not have this permission. Let’s ask the user
            permList.add(Manifest.permission.READ_PHONE_STATE)
            viewPermList.add(permissionView("Read Phone State", false))
        }
        else {
            viewPermList.add(permissionView("Read Phone State", true))
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)!= PackageManager.PERMISSION_GRANTED) {
            Log.d("Ö", "Receive SMS---------")
// We do not have this permission. Let’s ask the user
            permList.add(Manifest.permission.RECEIVE_SMS)
            viewPermList.add(permissionView("Receive SMS", false))
        }
        else {
            viewPermList.add(permissionView("Receive SMS", true))
        }

       /* if(ContextCompat.checkSelfPermission(this, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)!= PackageManager.PERMISSION_GRANTED) {

// We do not have this permission. Let’s ask the user
            permList.add(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
            viewPermList.add(permissionView("Notification Listener", false))
        }
        else {
            viewPermList.add(permissionView("Notification Listener", true))
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            if(ContextCompat.checkSelfPermission(this, "com.google.android.gms.permission.ACTIVITY_RECOGNITION")!= PackageManager.PERMISSION_GRANTED) {

                    permList.add("com.google.android.gms.permission.ACTIVITY_RECOGNITION")
                    viewPermList.add(permissionView("Google Activity Recognition", false))
            } else {
                viewPermList.add(permissionView("Google Activity Recognition", true))
            }
        }
            else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACTIVITY_RECOGNITION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("Ö", "223inninsdlasd")
                    // We do not have this permission. Let’s ask the user
                    permList.add(Manifest.permission.ACTIVITY_RECOGNITION)
                    viewPermList.add(permissionView("Google Activity Recognition", false))
                } else {
                    viewPermList.add(permissionView("Google Activity Recognition", true))
                }
            }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {

// We do not have this permission. Let’s ask the user
            permList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            viewPermList.add(permissionView("Location", false))
        }
        else {
            viewPermList.add(permissionView("Location", true))
        }

        if (!Applications.isAccessibilityServiceActive(this)) {
            viewPermList.add(permissionView("Accessibility Service", false))
        }
        else {
            viewPermList.add(permissionView("Accessibility Service", true))
        }

        Aware.startAWARE(this)
Log.d("Ö", permList.toString())
        ActivityCompat.requestPermissions(this@MainActivity, permList.toTypedArray(), MY_PERMISSIONS_REQUEST);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (!NotificationManagerCompat.getEnabledListenerPackages(this)
                    .contains(packageName)
            ) {        //ask for permission
                val intent =
                    Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(intent)
                viewPermList.add(permissionView("Notification Listener", false))
            }
            else {
                viewPermList.add(permissionView("Notification Listener", true))
            }
        }

        startService(Intent(this@MainActivity, InterruptionStudyService::class.java))

//Permission List View
        viewManager = LinearLayoutManager(this)
        viewAdapter = PermissionAdapter(viewPermList, this)

        recyclerView = findViewById<RecyclerView>(R.id.permission_list).apply {

            setHasFixedSize(true)

            layoutManager = viewManager

            adapter = viewAdapter

        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                //setContentView(R.layout.activity_timeline)
                val myIntent = Intent(this, SessionListView::class.java)
                startActivityForResult(myIntent, 0)
            }
            R.id.nav_home -> {
                val myIntent = Intent(this, MainActivity::class.java)
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

data class permissionView(
    val type: String = "None",
    val isGranted: Boolean = false

)


