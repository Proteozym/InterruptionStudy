package de.lmu.js.interruptionesm


import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import org.qap.ctimelineview.TimelineRow
import org.qap.ctimelineview.TimelineViewAdapter
import java.util.*
import kotlin.collections.ArrayList


class TimelineActivity: AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

     lateinit var toolbar: androidx.appcompat.widget.Toolbar
     lateinit var drawerLayout: DrawerLayout
     lateinit var navView: NavigationView

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

        createTimeline()

    }

     private fun createTimeline() {
         // Create Timeline rows List
         // Create Timeline rows List
         val timelineRowsList: ArrayList<TimelineRow> = ArrayList()

         // Create new timeline row (Row Id)
         val myRow = TimelineRow(0)

         // To set the row Date (optional)
         myRow.date = Date()

         // To set the row Title (optional)
         myRow.title = "Title"

         // To set the row Description (optional)
         myRow.description = "Description"

         // To set the row bitmap image (optional)
         myRow.image = BitmapFactory.decodeResource(resources, de.lmu.js.interruptionesm.R.drawable.ic_action_esm)

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
         timelineRowsList.add(myRow)
         timelineRowsList.add(myRow)


         // Create the Timeline Adapter
         val myAdapter: ArrayAdapter<TimelineRow> = TimelineViewAdapter(
             this, 0, timelineRowsList,  //if true, list will be sorted by date
             false
         )


         // Get the ListView and Bind it with the Timeline Adapter
         val myListView: ListView =
             findViewById<View>(R.id.timeline_listView) as ListView
         myListView.setAdapter(myAdapter)
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


 }