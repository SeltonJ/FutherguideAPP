package com.example.futherguideapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.futherguideapp.R
import com.example.futherguideapp.models.UserBirdObservation

class BirdObservationAdapter(private val birdList: MutableList<UserBirdObservation> = mutableListOf()) : RecyclerView.Adapter<BirdObservationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val birdName: TextView = itemView.findViewById(R.id.txt_birdComName)
        val birdCount: TextView = itemView.findViewById(R.id.txt_NumberOfBirdViewed)
        val date: TextView = itemView.findViewById(R.id.txt_observationDate)
        val birdLocation: TextView = itemView.findViewById(R.id.txt_location)
    }

    // Add this method inside your BirdObservationAdapter class
    fun updateData(newBirdObservations: List<UserBirdObservation>) {
        birdList.clear()
        birdList.addAll(newBirdObservations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bird_observation_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentBird = birdList[position]
        holder.birdName.text = currentBird.birdName
        holder.birdCount.text = currentBird.quantity.toString()
        holder.birdLocation.text = currentBird.location
        holder.date.text = currentBird.observationDate
    }

    override fun getItemCount() = birdList.size
}
