package com.example.synaera

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.FragmentChatBinding

class ChatFragment() : Fragment() {

    private lateinit var binding : FragmentChatBinding
    private var list : ArrayList<ChatBubble> =  ArrayList()
    private var mAdapter : RecyclerAdapter = RecyclerAdapter(list)

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
        mAdapter = RecyclerAdapter(list)
        binding.chatRV.adapter = mAdapter

        binding.editText.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                addItem(ChatBubble(binding.editText.text.toString(), false))
                binding.editText.setText("")
                return@OnKeyListener true
            }
            false
        })
    }

    fun addItem (item: ChatBubble) {
        list.add(item)
        mAdapter.notifyItemInserted(list.size - 1)
        //binding.chatRV.adapter = mAdapter
    }
}