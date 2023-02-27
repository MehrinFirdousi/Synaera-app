package com.example.synaera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.FragmentChatBinding

class ChatFragment() : Fragment() {

    lateinit var binding : FragmentChatBinding
    lateinit var list : ArrayList<ChatBubble>
    lateinit var mAdapter : RecyclerAdapter

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
    }

    fun addItem (item: ChatBubble) {
        list.add(item)
        mAdapter.notifyItemInserted(list.size - 1)
    }
}