package de.lmu.js.interruptionesm


import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
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
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.threetenabp.AndroidThreeTen
import de.lmu.js.interruptionesm.utilities.Encrypt.Companion.encryptKey
import de.lmu.js.interruptionesm.utilities.SessionUtil
import de.lmu.js.interruptionesm.utilities.SessionUtil.Companion.checkKey
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


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
    var accessibilityRequestReceiver: BroadcastReceiver? = null

    lateinit var prefFile: String
    lateinit var sharedPref: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    var checkSpinnerInit = 0
    var userKey: String = ""

    var mServiceIntent: Intent? = null
    private var mSensorService: InterruptionStudyService? = null
    var ctx: Context? = null

    var permissionToTrack = false
    var surveyFin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        prefFile = getString(R.string.preference_key)
        sharedPref = this.getSharedPreferences(
            prefFile, Context.MODE_PRIVATE)
        editor = sharedPref.edit();

        try {
            userKey = encryptKey(Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID))
        } catch (e: java.lang.Exception) {Log.e("Ö", "Error Encrypting")}


        // Initialize Firebase Auth
        var auth = FirebaseAuth.getInstance()

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        //updateUI(currentUser)

        accessibilityRequestReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action.equals("TRIGGER_ACCESSIBILITY")) {
                    var intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    ContextCompat.startActivity(this@MainActivity, intent, null)
                }
                if (intent.action.equals("TRIGGER_PERMISSION")) {
                    var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    var uri = Uri.fromParts("package", "de.lmu.js.interruptionesm", null);
                    intent.setData(uri);
                    ContextCompat.startActivity(this@MainActivity, intent, null)
                }
            }
        }

        var accessibilityRequestReceiverFilter = IntentFilter();
        accessibilityRequestReceiverFilter.addAction("TRIGGER_ACCESSIBILITY")
        accessibilityRequestReceiverFilter.addAction("TRIGGER_PERMISSION")
        registerReceiver(accessibilityRequestReceiver, accessibilityRequestReceiverFilter)

        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Ö", "signInAnonymously:success")
                    val user = auth.currentUser

                    DatabaseRef.addUserToSurvey(userKey, this)

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
        permissionToTrack = SessionUtil.checkPermSurvey(userKey, this)
        surveyFin = SessionUtil.checkSurveyFin(userKey, this)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, 0, 0
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        val usrTextView: TextView = navView.getHeaderView(0).findViewById(R.id.usrKey)
        val copyKey: ImageButton = navView.getHeaderView(0).findViewById(R.id.usrKeyBtn)

        usrTextView.text = userKey

        copyKey.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                var clip = ClipData.newPlainText("Key", usrTextView.text)
                clipboard.setPrimaryClip(clip)
            }

        })

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED) {
            Log.d("Ö", "Phone State---------")
            // We do not have this permission. Let’s ask the user
            permList.add(Manifest.permission.READ_PHONE_STATE)
            viewPermList.add(permissionView("Read Phone State", false, Manifest.permission.READ_PHONE_STATE))
        }
        else {
            viewPermList.add(permissionView("Read Phone State", true, Manifest.permission.READ_PHONE_STATE))
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)!= PackageManager.PERMISSION_GRANTED) {
            Log.d("Ö", "Receive SMS---------")
            // We do not have this permission. Let’s ask the user
            permList.add(Manifest.permission.RECEIVE_SMS)
            viewPermList.add(permissionView("Receive SMS", false, Manifest.permission.RECEIVE_SMS))
        }
        else {
            viewPermList.add(permissionView("Receive SMS", true, Manifest.permission.RECEIVE_SMS))
        }

        if (Build.VERSION.SDK_INT <= 28) {
            Log.d("Ö", "Q")
            if(ContextCompat.checkSelfPermission(this, "com.google.android.gms.permission.ACTIVITY_RECOGNITION")!= PackageManager.PERMISSION_GRANTED) {
                    permList.add("com.google.android.gms.permission.ACTIVITY_RECOGNITION")
                    viewPermList.add(permissionView("Google Activity Recognition", false, "com.google.android.gms.permission.ACTIVITY_RECOGNITION"))
            } else {
                viewPermList.add(permissionView("Google Activity Recognition", true, "com.google.android.gms.permission.ACTIVITY_RECOGNITION"))
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
                viewPermList.add(permissionView("Google Activity Recognition", false, Manifest.permission.ACTIVITY_RECOGNITION))
            } else {
                viewPermList.add(permissionView("Google Activity Recognition", true, Manifest.permission.ACTIVITY_RECOGNITION))
            }
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            // We do not have this permission. Let’s ask the user
            permList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            viewPermList.add(permissionView("Location", false, Manifest.permission.ACCESS_FINE_LOCATION))
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



        AndroidThreeTen.init(this);
        Aware.startScreen(this)
        Aware.startESM(this)

        //Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, true)

        // Register for checking application use
        //ware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true)
        //Aware.setSetting(this, Aware_Preferences.STATUS_NOTIFICATIONS, true)
        //Aware.startAWARE(this)

        if (permList.isNotEmpty()) ActivityCompat.requestPermissions(this@MainActivity, permList.toTypedArray(), MY_PERMISSIONS_REQUEST);

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


        //ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, InterruptionStudyService::class.java))
        //startService(Intent(this@MainActivity, InterruptionStudyService::class.java))

//Permission List View
        viewManager = LinearLayoutManager(this)
        viewAdapter = PermissionAdapter(viewPermList, this@MainActivity)

        recyclerView = findViewById<RecyclerView>(R.id.permission_list).apply {

            setHasFixedSize(true)

            layoutManager = viewManager

            adapter = viewAdapter

        }

        var app = sharedPref.getString("APP", "empty");
        Log.d("Ö is", app.toString())
        var spinnerSelected = AppItem<String>("null", "null")
        if (!app.equals("empty")) {
            var item = app?.split("|")
            spinnerSelected = AppItem<String>(item?.get(0)!!, item?.get(1)!!)
            Log.d("Ö is", spinnerSelected.toString())
        }

        var appData = installedApps()

        val spinner: Spinner = findViewById(R.id.app_spinner)

        val positionAdapter = ArrayAdapter<AppItem<String>>(this, android.R.layout.simple_spinner_item, appData).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        // Set layout to use when the list of choices appear
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Set Adapter to Spinner
        spinner!!.setAdapter(positionAdapter)
        //Read from save
        spinner!!.setSelection(positionAdapter.getPosition(spinnerSelected))

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // val item = parent.getItemAtPosition(position) as EnumTextItem<Position>
                if(++checkSpinnerInit > 1) {
                    val item = positionAdapter.getItem(position)

                    editor.putString("APP", item?.packageName + "|" + item?.text)

                    editor.apply();

                    var succ = editor.commit();
                    Log.d("Ö in", succ.toString())
                }
            }
        }
        var dialog = BatteryOptimizationUtil.getBatteryOptimizationDialog(this);
        if (dialog != null) dialog.show();

        mServiceIntent = Intent(this, InterruptionStudyService::class.java)
        if (!isMyServiceRunning(InterruptionStudyService::class.java)) {
            startService(mServiceIntent)
        }


        var submitForm = findViewById<View>(R.id.submitKeyForm) as LinearLayout
        var submitButton = findViewById<View>(R.id.submitKey) as Button
        var submitText = findViewById<View>(R.id.submitText) as TextView
        var defText = findViewById<View>(R.id.survDefault) as TextView
        var initSurveyButton = findViewById<View>(R.id.initSurvey) as Button
        var finSurveyButton = findViewById<View>(R.id.finSurvey) as Button

        submitButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                //Check Key
                var keyVal = checkKey(submitText.text.toString(), userKey, this@MainActivity)


                if (keyVal) {
                    Toast.makeText(
                        this@MainActivity,
                        "Key valid, survey has started!",
                        Toast.LENGTH_LONG
                    ).show()
                    submitForm.setVisibility(View.GONE)
                    submitButton.setVisibility(View.GONE)
                    submitText.setVisibility(View.GONE)
                    defText.setVisibility(View.VISIBLE)
                    initSurveyButton.setVisibility(View.GONE)
                    finSurveyButton.setVisibility(View.GONE)

                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Key invalid, please try again!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                submitText.text = ""
                keyVal = false
                //initSurveyButton.setVisibility(View.GONE)
                //stopButton.setVisibility(View.VISIBLE)
            }
        })

        if (!permissionToTrack) {
            defText.setVisibility(View.GONE)


            initSurveyButton.setVisibility(View.VISIBLE)
            submitForm.setVisibility(View.VISIBLE)
            submitButton.setVisibility(View.VISIBLE)
            submitText.setVisibility(View.VISIBLE)
            initSurveyButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {

                    //Open survey with key
                    val openURL = Intent(android.content.Intent.ACTION_VIEW)
                    openURL.data = Uri.parse("https://www.soscisurvey.de/interruptionstudy/?q=qnr1&key=".plus(userKey))
                    startActivity(openURL)
                    //initSurveyButton.setVisibility(View.GONE)
                    //stopButton.setVisibility(View.VISIBLE)
                }
            })

        }
        else if (surveyFin) {
            defText.setVisibility(View.GONE)

            finSurveyButton.setVisibility(View.VISIBLE)
            submitForm.setVisibility(View.VISIBLE)
            submitButton.setVisibility(View.VISIBLE)
            submitText.setVisibility(View.VISIBLE)
            finSurveyButton.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    val openURL = Intent(android.content.Intent.ACTION_VIEW)
                    openURL.data = Uri.parse("https://www.soscisurvey.de/interruptionstudy/?q=qnr2&key=".plus(userKey))
                    startActivity(openURL)
                    //Open survey with key
                    //finSurveyButton.setVisibility(View.GONE)
                    //stopButton.setVisibility(View.VISIBLE)
                }
            })
        }
        else {
            defText.setVisibility(View.VISIBLE)
        }

    }
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager: ActivityManager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.getClassName()) {
                Log.i("isMyServiceRunning?", true.toString() + "")
                return true
            }
        }
        Log.i("isMyServiceRunning?", false.toString() + "")
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
       // when (requestCode) {
           // MY_PERMISSIONS_REQUEST_READ_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(getIntent());
                    overridePendingTransition(0, 0);
                    //viewAdapter.notifyItemChanged(1)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            //}

            // Add other 'when' lines to check for other
            // permissions this app might request.
           // else -> {
                // Ignore all other requests.
          //  }
       // }
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
        //startService(Intent(this@MainActivity, InterruptionStudyService::class.java))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RestartServiceBroadcastReceiver.scheduleJob(applicationContext)
        } else {
            val bck = ProcessMainClass()
            bck.launchService(applicationContext)
        }
        var accessibilityRequestReceiverFilter = IntentFilter();
        accessibilityRequestReceiverFilter.addAction("TRIGGER_ACCESSIBILITY")
        accessibilityRequestReceiverFilter.addAction("TRIGGER_PERMISSION")
        registerReceiver(accessibilityRequestReceiver, accessibilityRequestReceiverFilter)
        permissionToTrack = SessionUtil.checkPermSurvey(userKey, this)
        surveyFin = SessionUtil.checkSurveyFin(userKey, this)


    }

    override fun onPause() {
       super.onPause()
        unregisterReceiver(accessibilityRequestReceiver)
    }

    override fun onStop() {
        super.onStop();
    }

    override fun onDestroy() {
        stopService(mServiceIntent);
        Log.i("MAINACT", "onDestroy!");
        super.onDestroy()
    }

    fun installedApps(): ArrayList<AppItem<String>> {
        val appList: ArrayList<AppItem<String>> = ArrayList()
        appList.add(AppItem("", ""))
        val packList: List<PackageInfo> = packageManager.getInstalledPackages(0)
        val sortedList = packList.sortedWith ( compareBy{(it.applicationInfo.loadLabel(packageManager).toString())  } )
        for (i in 0 until sortedList.size) {
            val packInfo: PackageInfo = sortedList[i]
            if ((packInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName: String =
                    packInfo.applicationInfo.loadLabel(packageManager).toString()
                appList.add(AppItem(packInfo.packageName, appName))

            }
        }
        return appList
    }
    fun checkForUpdate() {
             AppUpdater(this)
            .setUpdateFrom(UpdateFrom.GITHUB)
             .setGitHubUserAndRepo("Proteozym", "InterruptionStudy")
            .start();
    }



}

data class permissionView(
    val type: String = "None",
    val isGranted: Boolean = false,
    var manifest: String = "None"
)


class AppItem<T>(val packageName: String, val text: String) {
    override fun toString(): String {
        return text
    }
    fun getPackage(): String {
        return packageName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        if (!text.equals(other.toString())) return false
        return true
    }


}



