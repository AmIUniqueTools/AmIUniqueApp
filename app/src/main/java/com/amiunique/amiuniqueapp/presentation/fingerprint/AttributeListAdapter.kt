package com.amiunique.amiuniqueapp.presentation.fingerprint

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amiunique.amiuniqueapp.R
import com.google.android.material.card.MaterialCardView
import java.util.*

class AttributeListAdapter(private var attributeListData: ArrayList<AttributeModel>) :
    RecyclerView.Adapter<AttributeListAdapter.ViewHolder>() {

    // Create a copy of localityList that is not a clone
    // (so that any changes in localityList aren't reflected in this list)
    private var initialAttributeListData = ArrayList<AttributeModel>().apply {
        addAll(attributeListData)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateInitialAttributeListData(data: ArrayList<AttributeModel>) {
        data.let {
            initialAttributeListData.addAll(it)
            attributeListData.addAll(it)
            notifyDataSetChanged()
        }
    }

    private val attributeFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: ArrayList<AttributeModel> = ArrayList()
            if (constraint.isNullOrEmpty()) {
                initialAttributeListData.let { filteredList.addAll(it) }
            } else {
                val query = constraint.toString().trim().lowercase(Locale.ROOT)
                initialAttributeListData.forEach {
                    if (it.attribute.lowercase(Locale.ROOT).contains(query) ||
                        it.value.lowercase(Locale.ROOT).contains(query) ||
                        it.description.lowercase(Locale.ROOT).contains(query)){
                        filteredList.add(it)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            (results?.values as? ArrayList<AttributeModel>)?.let {
                attributeListData.apply {
                    clear()
                    addAll(it)
                }
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_attribute, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.attributeNameTV.text = attributeListData[position].attribute
        holder.attributeDescriptionTV.text = attributeListData[position].description
        holder.attributeValueTV.text = attributeListData[position].value

        holder.attributeCardView.setOnClickListener {
            attributeListData[position].showDetails = !attributeListData[position].showDetails
            holder.attributeValueTV.maxLines =
                if (attributeListData[position].showDetails) Int.MAX_VALUE else 1
        }
    }

    override fun getItemCount(): Int {
        return attributeListData.size
    }

    fun getFilter(): Filter {
        return attributeFilter
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val attributeCardView: MaterialCardView =
            itemView.findViewById(R.id.applicationCard)
        internal val attributeNameTV: TextView = itemView.findViewById(R.id.attributeName)
        internal val attributeDescriptionTV: TextView = itemView.findViewById(R.id.attributeDescription)
        internal val attributeValueTV: TextView = itemView.findViewById(R.id.attributeValue)
    }
}