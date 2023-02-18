package com.example.synaera

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(var fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int {
        return (3)
    }
    override fun createFragment(position: Int): Fragment {
        Log.d("position msg: ", "new position = $position")
        return when(position) {
            0 -> FilesFragment.newInstance()
            2 -> ChatFragment.newInstance()
            else -> FilesFragment.newInstance()
        }
    }

}