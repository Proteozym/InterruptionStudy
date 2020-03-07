package de.lmu.js.interruptionesm

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.navigation.NavigationView
import com.google.common.base.Strings.isNullOrEmpty
import com.google.firebase.firestore.*
import com.google.firebase.firestore.auth.User
import de.lmu.js.interruptionesm.DatabaseRef.populateListWithSessionsForUserKey

import kotlinx.coroutines.MainScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_list.*

class SessionListView : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit private var mMainList: RecyclerView
    private val dbInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    lateinit var toolbar: androidx.appcompat.widget.Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    lateinit var userKey: String
    var list: ListView? = null
    private var adapter: ProductFirestoreRecyclerAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list)

        userKey = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)

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

        //LIST
        //TODO read DB

        if (!isNullOrEmpty(userKey)) {
            mMainList = findViewById(R.id.main_list)
            mMainList.layoutManager = LinearLayoutManager(this)

            val itemOnClick: (View, Int, Int) -> Unit = { view, position, type ->


            }

            val query = dbInstance!!.collection("UserEvent").whereEqualTo("userKey", userKey).whereEqualTo("eventType", "SESSION_START")//.orderBy("productName", Query.Direction.ASCENDING)

            val options = FirestoreRecyclerOptions.Builder<UserEvent>().setQuery(query, UserEvent::class.java).build()

            adapter = ProductFirestoreRecyclerAdapter(options, itemOnClick)
            mMainList.adapter = adapter


        }


    }
    override fun onStart() {
        super.onStart()
        adapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()

        if (adapter != null) {
            adapter!!.stopListening()
        }
    }
//TimelineActivity::class.java
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




    private inner class ProductFirestoreRecyclerAdapter internal constructor(options: FirestoreRecyclerOptions<UserEvent>, val itemClickListener: (View, Int, Int) -> Unit) : FirestoreRecyclerAdapter<UserEvent, ProductFirestoreRecyclerAdapter.SessionViewHolder>(options) {
        override fun onBindViewHolder(productViewHolder: SessionViewHolder, position: Int, sessionModel: UserEvent) {
            productViewHolder.setSession(sessionModel.timestamp.toString())
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
            SessionViewHolder(view).onClick(itemClickListener)
            return SessionViewHolder(view)
        }

        inner class SessionViewHolder (val view: View) : RecyclerView.ViewHolder(view) {

            internal fun setSession(session: String) {
                val textView = view.findViewById<TextView>(R.id.text_view)
                textView.text = session
            }

        }
    }

    fun <T : RecyclerView.ViewHolder> T.onClick(event: (view: View, position: Int, type: Int) -> Unit): T {
        itemView.setOnClickListener {
            event.invoke(it, getAdapterPosition(), getItemViewType())
        }
        return this
    }

}