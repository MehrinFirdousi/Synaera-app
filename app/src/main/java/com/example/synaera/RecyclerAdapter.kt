package com.example.synaera

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.ChatItemBinding

open class RecyclerAdapter(var items: ArrayList<ChatBubble>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    private lateinit var context : Context
    inner class ViewHolder(b: ChatItemBinding) : RecyclerView.ViewHolder(b.root) {
        var binding: ChatItemBinding = b


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            ChatItemBinding.inflate(
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
        val params = holder.binding.chatItemText.layoutParams as ConstraintLayout.LayoutParams

        val currItem = items[position]
        holder.binding.chatItemText.text = currItem.text
        if (currItem.sender) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            holder.binding.chatItemText.setBackgroundColor(ContextCompat.getColor(context, R.color.charcoal))
        } else {
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
    }
}