package com.example.futherguideapp.adapters
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.futherguideapp.BirdDetails
import com.example.futherguideapp.R
import com.example.futherguideapp.models.BirdObservation
import kotlin.concurrent.thread

class MyBirdAdapter(private var birdList: MutableList<BirdObservation>):
    RecyclerView.Adapter<MyBirdAdapter.MyViewHolder>(){

    var onBirdCardClick:((BirdObservation) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Log.d("Adapter", "onCreateViewHolder called")
        val birdView = LayoutInflater.from(parent.context).inflate(R.layout.bird_list,
            parent, false)
        return MyViewHolder(birdView)
    }

    fun setFilteredList(birdList: MutableList<BirdObservation>){
        this.birdList = birdList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return birdList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentBird = birdList[position]

        thread {

            // Create a handler associated with the main (UI) thread
            val handler = Handler(Looper.getMainLooper())

            // Post a runnable to the main thread to update the UI
            handler.post {
                // Make specific parts of the text bold using HTML formatting
                val locationText = "<b>Bird location:</b> ${currentBird.locName}."
                val sciNameText = "<b>Specie name:</b> ${currentBird.sciName}."
                val howManyText = "<b>Number of birds:</b> ${currentBird.howMany}."

                holder.birdComName.text = currentBird.comName
                holder.birdsSciName.text = Html.fromHtml(sciNameText)
                holder.birdLocation.text = Html.fromHtml(locationText)
                holder.birdHowMany.text = Html.fromHtml(howManyText)

                //sending details to bird details activity
                holder.itemView.setOnClickListener {
                    val intent = Intent(holder.itemView.context, BirdDetails::class.java)
                    intent.putExtra("selectedBird", currentBird)
                    holder.itemView.context.startActivity(intent)
                }
            }
        }
    }

    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val birdsSciName: TextView = itemView.findViewById(R.id.txt_birdsSciName)
        val birdLocation: TextView = itemView.findViewById(R.id.txt_birdLocation)
        val birdComName: TextView = itemView.findViewById(R.id.txt_birdComName)
        val birdHowMany: TextView = itemView.findViewById(R.id.txt_birdHowMany)
    }
}