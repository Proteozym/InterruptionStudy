package de.lmu.js.interruptionesm


import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import org.qap.ctimelineview.TimelineRow
import org.qap.ctimelineview.TimelineViewAdapter
import org.threeten.bp.DateTimeUtils
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import java.lang.String.format
import java.text.SimpleDateFormat
import java.time.Instant.now
import java.util.*
import kotlin.collections.ArrayList


class TimelineActivity: AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    lateinit private var mMainList: RecyclerView
    private val dbInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    var sessionId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline)


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

        var bundle = intent.extras
        sessionId = bundle?.getInt("sessionId")!!

        Log.d("iniit", sessionId.toString())

        createTimeline()

    }

     private fun createTimeline() {

         val timelineRowsList: ArrayList<TimelineRow> = ArrayList()
         readData(object : MyCallback {
             override fun onCallback(eventList: List<UserEvent?>?, context: Context) {

                 if (eventList != null) {
                     Log.d("noway..", eventList.toString())

                     for (event in eventList.sortedBy { it?.timestamp?.toDate() }) {

                         // Create new timeline row (Row Id)
                         val myRow = TimelineRow(0)

                         // To set the row Date (optional)
                         Log.d("noway..", event?.timestamp?.toDate().toString())
                         //myRow.date =  event?.timestamp?.toDate()
                         // To set the row Title (optional)
                         val sdf = SimpleDateFormat("hh:mm:ss")
                         myRow.title = event?.eventType.toString() + " (" + sdf.format(event?.timestamp?.toDate()) + ")"

                         // To set the row Description (optional)
                         val eveVal = event?.eventValue
                         if (eveVal != eventValue.NONE) {
                             myRow.description = event?.eventValue.toString()
                         }

                         // To set row Below Line Color (optional)
                         myRow.bellowLineColor = Color.argb(255, 44, 44, 44)

                         // To set row Below Line Size in dp (optional)
                         myRow.bellowLineSize = 4

                         // To set background color of the row image (optional)



                         when (event?.eventType) {
                             eventType.SESSION_START -> {
                                 myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_radio_button_checked_black_24dp)?.toBitmap()

                                 myRow.imageSize = 35
                                 myRow.backgroundSize = 50
                             }
                             eventType.SESSION_END -> {
                                 myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_radio_button_unchecked_black_24dp)?.toBitmap()

                                 myRow.imageSize = 35
                                 myRow.backgroundSize = 50
                             }
                             eventType.INTERRUPTION_START -> {
                                 myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_arrow_forward_black_24dp)?.toBitmap()

                                 myRow.imageSize = 20
                                 myRow.backgroundSize = 25

                                 if (event.additionalProps.containsKey("switchedTo")) myRow.description = myRow.description + "\n(" + event.additionalProps.get("switchedTo") +")"

                                 if (event.additionalProps.get("receivedCall") == "true") {
                                     val comRow = TimelineRow(0)
                                     comRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_call_black_24dp)?.toBitmap()
                                     comRow.imageSize = 10
                                     comRow.backgroundSize = 12
                                     comRow.title = "Call Received"
                                     timelineRowsList.add(comRow)
                                 }
                                 if (event.additionalProps.get("receivedMessage") == "true") {
                                     val comRow = TimelineRow(0)
                                     comRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_message_black_24dp)?.toBitmap()
                                     comRow.imageSize = 10
                                     comRow.backgroundSize = 12
                                     comRow.title = "Message Received"
                                     timelineRowsList.add(comRow)
                                 }

                             }
                             eventType.INTERRUPTION_END -> {
                                 myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_arrow_back_black_24dp)?.toBitmap()
                                 myRow.imageSize = 20
                                 myRow.backgroundSize = 25
                             }
                             eventType.ESM_SENT -> {
                                 myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_poll_black_24dp)?.toBitmap()
                                 myRow.imageSize = 20
                                 myRow.backgroundSize = 25
                             }
                             eventType.ESM_ANSWER -> {
                                 myRow.image = AppCompatResources.getDrawable(context, R.drawable.poll_received)?.toBitmap()
                                 myRow.imageSize = 20
                                 myRow.backgroundSize = 25

                             }
                             eventType.ESM_EXPIRED -> {
                                 myRow.image = AppCompatResources.getDrawable(context, R.drawable.poll_timedout)?.toBitmap()
                                 myRow.imageSize = 20
                                 myRow.backgroundSize = 25

                             }

                             eventType.MOVEMENT -> {

                                 when (event?.eventValue) {
                                     eventValue.WALKING -> {
                                         myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_directions_walk_black_24dp)?.toBitmap()
                                     }
                                     eventValue.RUNNING -> {
                                         myRow.image = AppCompatResources.getDrawable(context,  R.drawable.ic_directions_run_black_24dp)?.toBitmap()
                                     }
                                     eventValue.STILL -> {
                                         myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_airline_seat_legroom_normal_black_24dp)?.toBitmap()

                                     }
                                     eventValue.IN_VEHICLE -> {
                                         myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_directions_car_black_24dp)?.toBitmap()
                                     }
                                     eventValue.BICYCLE -> {
                                         myRow.image = AppCompatResources.getDrawable(context, R.drawable.ic_on_bicycle)?.toBitmap()
                                     }
                                 }

                                 myRow.imageSize = 20
                                 myRow.backgroundSize = 25

                             }




                         }

                         myRow.backgroundColor = Color.argb(255, 47, 133, 76)
                         // To set row Date text color (optional)
                         myRow.dateColor = Color.argb(255, 0, 0, 0)

                                 // To set row Title text color (optional)
                         myRow.titleColor = Color.argb(255, 0, 0, 0)

                             // To set row Description text color (optional)
                         myRow.descriptionColor = Color.argb(255, 0, 0, 0)


                         // Add the new row to the list
                         timelineRowsList.add(myRow)
                     }
                     val myAdapter: ArrayAdapter<TimelineRow> = TimelineViewAdapter(
                         context, 0, timelineRowsList,  //if true, list will be sorted by date
                         false
                     )


                     // Get the ListView and Bind it with the Timeline Adapter
                     val myListView: ListView =
                         findViewById<View>(R.id.timeline_listView) as ListView
                     myListView.setAdapter(myAdapter)
                 }

             }
         })

         // Create the Timeline Adapter

     }

     override fun onNavigationItemSelected(item: MenuItem): Boolean {
         when (item.itemId) {
             R.id.nav_home -> {
                 val myIntent = Intent(this, MainActivity::class.java)
                 startActivityForResult(myIntent, 0)
             }
         }
         drawerLayout.closeDrawer(GravityCompat.START)
         return true
     }

    interface MyCallback {
        fun onCallback(attractionsList: List<UserEvent?>?, context: Context)
    }

    fun readData(myCallback: MyCallback) {
        dbInstance.collection("UserEvent").whereEqualTo("sessionId", sessionId).get().addOnCompleteListener(OnCompleteListener<QuerySnapshot> { task ->
            if (task.isSuccessful) {
                val attractionsList: MutableList<UserEvent?> = ArrayList()
                for (document in task.result!!) {
                    val uEve: UserEvent = document.toObject(UserEvent::class.java)
                    attractionsList.add(uEve)
                }
                myCallback.onCallback(attractionsList, this)
            }
        })
    }


 }