package com.example.synaera

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.synaera.databinding.FragmentFilesBinding
import java.io.File

class FilesFragment() : Fragment() {

    private var list: ArrayList<VideoItem> = ArrayList()
    var mAdapter = VideoRecyclerAdapter(list)
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
        val mainActivity = requireActivity() as MainActivity

        binding.uploadButton.setOnClickListener {
            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_PICK
            mainActivity.selectVideoIntent.launch(intent)
        }
    }

    fun addItem (item: VideoItem) {
        list.add(item)
        mAdapter.notifyItemInserted(list.size - 1)

    }
    fun changeStatus(status : String) {
        val lastPos = binding.videoRV.adapter?.itemCount?.minus(1)
        list[lastPos!!].status = status
        mAdapter.notifyItemChanged(lastPos)
    }

    fun addTranscript(transcript: String) {
        val lastPos = binding.videoRV.adapter?.itemCount?.minus(1)
        list[lastPos!!].transcript = transcript
        mAdapter.notifyItemChanged(lastPos)
    }

}