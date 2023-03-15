package com.example.synaera

import android.content.Context
import android.view.LayoutInflater

import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.VideoItemBinding

open class VideoRecyclerAdapter(var items: ArrayList<VideoItem>):
    RecyclerView.Adapter<VideoRecyclerAdapter.ViewHolder>() {

    private lateinit var context : Context

    inner class ViewHolder(b: VideoItemBinding) : RecyclerView.ViewHolder(b.root) {
        var binding = b
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context

        return ViewHolder(
            VideoItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }
        override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currItem = items[position]

        holder.binding.imageView.setImageBitmap(currItem.image)
        holder.binding.titleTextView.text = currItem.title
        holder.binding.statusTextView.text = currItem.status
    }

}