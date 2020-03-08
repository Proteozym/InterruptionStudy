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
import androidx.cardview.widget.CardView
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

import kotlinx.coroutines.MainScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_list.*
import java.text.SimpleDateFormat

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


            val query = dbInstance!!.collection("UserEvent").whereEqualTo("userKey", userKey).whereEqualTo("eventType", "SESSION_START")//.orderBy("productName", Query.Direction.ASCENDING)

            val options = FirestoreRecyclerOptions.Builder<UserEvent>().setQuery(query, UserEvent::class.java).build()

            adapter = ProductFirestoreRecyclerAdapter(options)

            mMainList.adapter = adapter
            mMainList.layoutManager = LinearLayoutManager(this)

            adapter?.itemClickListener = { event ->

                // do something with your item
                Log.d("TAG", event.sessionId.toString())
                val myIntent = Intent(this, TimelineActivity::class.java)
                var bundle = Bundle()
                bundle.putInt("sessionId", event.sessionId)
                myIntent.putExtras(bundle)
                startActivityForResult(myIntent, 0)
            }


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







}

class ProductFirestoreRecyclerAdapter internal constructor(options: FirestoreRecyclerOptions<UserEvent>) : FirestoreRecyclerAdapter<UserEvent, ProductFirestoreRecyclerAdapter.SessionViewHolder>(options) {

    // var options: FirestoreRecyclerOptions<UserEvent>? = null
    // init {
    var itemClickListener: ((UserEvent) -> Unit)? = null
    var events: List<UserEvent>? = null
    //}
    override fun onBindViewHolder(productViewHolder: ProductFirestoreRecyclerAdapter.SessionViewHolder, position: Int, userEvent: UserEvent) {
        productViewHolder.setSessName(userEvent)
        /*val textView = productViewHolder.view.findViewById<TextView>(R.id.text_view)

        val eve = events?.get(position)
        textView.text = userEvent.timestamp.toString()*/
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)

        return SessionViewHolder(view)
    }

    inner class SessionViewHolder (val view: View) : RecyclerView.ViewHolder(view) {
        internal fun setSessName(userEvent: UserEvent) {
            val textView = view.findViewById<TextView>(R.id.text_view)
            val card = view.findViewById<CardView>(R.id.card_view)
            textView.text = "Session"

            card.setOnClickListener {
                itemClickListener?.invoke(userEvent)
            }
            val sdf = SimpleDateFormat("EE - dd MMMM hh:mm")
            val itemDate = view.findViewById<TextView>(R.id.itemDate)
            itemDate.text = sdf.format(userEvent.timestamp.toDate())
        }
        /*init {
            itemView.setOnClickListener {
                itemClickListener?.invoke(events!![adapterPosition])
            }
        }*/
    }

  /*  override fun getItemCount(): Int {
        return 0
    }*/
}