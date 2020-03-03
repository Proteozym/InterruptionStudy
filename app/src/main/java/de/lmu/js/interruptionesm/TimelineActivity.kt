package de.lmu.js.interruptionesm

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

 class TimelineActivity: AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

     lateinit var toolbar: androidx.appcompat.widget.Toolbar
     lateinit var drawerLayout: DrawerLayout
     lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

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