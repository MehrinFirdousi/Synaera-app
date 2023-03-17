package com.example.synaera

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.FragmentFilesBinding

class FilesFragment() : Fragment() {

    private var list: ArrayList<VideoItem> = ArrayList()
    private var mAdapter = VideoRecyclerAdapter(list)
    private lateinit var binding : FragmentFilesBinding

    constructor(list : ArrayList<VideoItem>) : this() {
        this.list = list
    }

    companion object {
        @JvmStatic
        fun newInstance(list : ArrayList<VideoItem>) : FilesFragment {
            return FilesFragment(list)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.videoRV.layoutManager = layoutManager

        mAdapter = VideoRecyclerAdapter(list)
        binding.videoRV.adapter = mAdapter
    }

    fun addItem (item: VideoItem) {
        list.add(item)
        mAdapter.notifyItemInserted(list.size - 1)

    }

    fun changeStatus(pos : Int, status : String) {
        list[pos].status = status
        mAdapter.notifyItemChanged(pos)
    }
}