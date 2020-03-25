package de.lmu.js.interruptionesm


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView


class PermissionAdapter(items: MutableList<permissionView>, context: Context): RecyclerView.Adapter<PermissionAdapter.ViewHolder>() {
    var values: MutableList<permissionView>
    var cont: Context
    init {
        values = items
        cont = context
    }
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    inner class ViewHolder(var layout: View) : RecyclerView.ViewHolder(layout) {
        // each data item is just a string in this case
        var txtHeader: TextView
        var txtFooter: TextView
        var icon: ImageView

        init {
            txtHeader = layout.findViewById<View>(R.id.firstLine) as TextView
            txtFooter = layout.findViewById<View>(R.id.secondLine) as TextView
            icon = layout.findViewById<View>(R.id.icon) as ImageView

        }
    }

    fun add(position: Int, item: permissionView) {
        values.add(position, item)
        notifyItemInserted(position)
    }

    fun remove(position: Int) {
        values.removeAt(position)
        notifyItemRemoved(position)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        // create a new view
        val inflater = LayoutInflater.from(
            parent.context
        )
        val v: View = inflater.inflate(R.layout.permission_list, parent, false)
        // set the view's size, margins, paddings and layout parameters
        return ViewHolder(v)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val item = values[position]
        holder.txtHeader.text = item.type
        holder.txtHeader.setOnClickListener {
            //remove(position)

        }
        if (item.isGranted) {
            holder.icon.setImageDrawable(AppCompatResources.getDrawable(cont, R.drawable.status_granted))
        }
        else {
            holder.icon.setImageDrawable(AppCompatResources.getDrawable(cont, R.drawable.status_denied))
        }
        //holder.txtFooter.text = "Footer: $name"
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return values.size
    }

}