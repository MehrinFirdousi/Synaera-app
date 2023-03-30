package com.example.synaera

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.FragmentChatBinding

class ChatFragment() : Fragment() {

    private lateinit var binding : FragmentChatBinding
    private var list : ArrayList<ChatBubble> =  ArrayList()
    private var mAdapter : ChatRecyclerAdapter = ChatRecyclerAdapter(ArrayList()) { _, _ ->}
    private var editing = false
    private var tempPos = 0

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

        val imm: InputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val mainActivity = requireActivity() as MainActivity
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        val viewPagerParams = mainActivity.viewBinding.viewPager.layoutParams as ConstraintLayout.LayoutParams

        binding.chatRV.layoutManager = layoutManager
        mAdapter = ChatRecyclerAdapter(list) { item, pos ->
            binding.editText.setText(item.text)
            editing = true
            tempPos = pos
            if(binding.editText.requestFocus()){
                imm.showSoftInput(binding.editText, InputMethodManager.SHOW_IMPLICIT)
                binding.editText.setSelection(binding.editText.length())
                println("scrolling to last msg")
                scrollToPos(list.size - 1)
                println("done")

                viewPagerParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                viewPagerParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                mainActivity.viewBinding.bottomNavBar.visibility = View.GONE
            }
        }

        binding.chatRV.adapter = mAdapter

        scrollToPos(mAdapter.itemCount - 1)

        binding.editText.setOnClickListener{
            viewPagerParams.bottomToTop = ConstraintLayout.LayoutParams.UNSET
            viewPagerParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            mainActivity.viewBinding.bottomNavBar.visibility = View.GONE
        }

        binding.sendBttn.setOnClickListener{
            setListener()
            viewPagerParams.bottomToTop = mainActivity.viewBinding.bottomNavBar.id
            viewPagerParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            mainActivity.viewBinding.bottomNavBar.visibility = View.VISIBLE
//            imm.hideSoftInputFromWindow(binding.editText.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }

        binding.chatFragment.addOnLayoutChangeListener { v, left, top, right, bottom, leftWas, topWas, rightWas, bottomWas ->
            val heightWas = bottomWas - topWas // Bottom exclusive, top inclusive
            if (v.height != heightWas) {
                scrollToPos(list.size - 1)
                println("height changed")
            }
        }

        binding.editText.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            scrollToPos(list.size - 1)
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                setListener()
                imm.hideSoftInputFromWindow(binding.editText.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                mainActivity.viewBinding.bottomNavBar.visibility = View.VISIBLE
                return@OnKeyListener true
            }
            false
        })

        scrollToPos(list.size - 1)

    }

    private fun scrollToPos(pos: Int) {
        (binding.chatRV.layoutManager as LinearLayoutManager).scrollToPosition(pos)
    }

    fun addItem (item: ChatBubble) {
        list.add(item)
        mAdapter.notifyItemInserted(list.size - 1)

    }

    private fun setListener() {
        val editString = binding.editText.text.toString().trim()
        if (editString != "") {
            if (!editing) {
                addItem(ChatBubble(editString, false))
                scrollToPos(list.size - 1)
            } else {
                list[tempPos].text = editString
                mAdapter.notifyItemChanged(tempPos)
                scrollToPos(tempPos)
                editing = false
            }
        }
        binding.editText.setText("")
    }
}