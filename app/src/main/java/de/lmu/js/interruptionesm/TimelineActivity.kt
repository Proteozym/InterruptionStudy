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
                         myRow.description = event?.eventValue.toString()

                         // To set the row bitmap image (optional)
                         myRow.image = BitmapFactory.decodeResource(
                             resources,
                             de.lmu.js.interruptionesm.R.drawable.ic_action_esm
                         )

                         // To set row Below Line Color (optional)
                         myRow.bellowLineColor = Color.argb(255, 0, 0, 0)

                         // To set row Below Line Size in dp (optional)
                         myRow.bellowLineSize = 6

                         // To set row Image Size in dp (optional)
                         myRow.imageSize = 40

                         // To set background color of the row image (optional)
                         myRow.backgroundColor = Color.argb(255, 0, 0, 0)

                         // To set the Background Size of the row image in dp (optional)
                         myRow.backgroundSize = 60

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