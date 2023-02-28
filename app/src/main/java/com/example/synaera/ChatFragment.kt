package com.example.synaera

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.ChatItemBinding
import com.example.synaera.databinding.FragmentChatBinding

class ChatFragment() : Fragment() {

    private lateinit var binding : FragmentChatBinding
    private var list : ArrayList<ChatBubble> =  ArrayList()
    private var mAdapter : RecyclerAdapter = RecyclerAdapter(list) { _, _ ->}
    private var editing = false

    companion object {
        @JvmStatic
        fun newInstance(list : ArrayList<ChatBubble>) : ChatFragment {
            return ChatFragment(list)
        }
    }

    constructor(list : ArrayList<ChatBubble>) : this() {
        this.list = list
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.chatRV.layoutManager = layoutManager
        mAdapter = RecyclerAdapter(list) {item, pos ->
            binding.editText.setText(item.text)
            editing = true

            binding.editText.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                    if (!editing) {
                        addItem(ChatBubble(binding.editText.text.toString(), false))
                    } else {
                        item.text = binding.editText.text.toString()
                        mAdapter.notifyItemChanged(pos)
                        editing = false
                    }
                    binding.editText.setText("")
                    return@OnKeyListener true
                }
                false
            })
        }
        binding.chatRV.adapter = mAdapter


    }

    fun addItem (item: ChatBubble) {
        list.add(item)
        mAdapter.notifyItemInserted(list.size - 1)
        //binding.chatRV.adapter = mAdapter
    }
}