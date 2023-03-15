package com.example.synaera

import com.example.synaera.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.synaera.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class HomeFragment : Fragment() {

    private lateinit var homeBinding: FragmentHomeBinding
    private lateinit var bottomNavigationView : BottomNavigationView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeBinding = FragmentHomeBinding.inflate(inflater, container, false)

        val mainActivity = requireActivity() as MainActivity
        bottomNavigationView = mainActivity.findViewById<BottomNavigationView>(R.id.bottom_nav_bar)

        homeBinding.translateHomeButton.setOnClickListener {
            bottomNavigationView.selectedItemId = R.id.camera_menu_id
        }
        homeBinding.chatHomeButton.setOnClickListener {
            bottomNavigationView.selectedItemId = R.id.chat_menu_id
        }
        homeBinding.uploadHomeButton.setOnClickListener {
            bottomNavigationView.selectedItemId = R.id.gallery_menu_id
        }
        homeBinding.profileImg.setOnClickListener {
            bottomNavigationView.selectedItemId = R.id.profile_menu_id
        }

//        val view = requireActivity().findViewById<View>(R.id.bottom_nav_bar)
//        homeBinding.cardView.shapeAppearanceModel = homeBinding.cardView.shapeAppearanceModel.toBuilder()
//            .setTopLeftCorner(CornerFamily.ROUNDED, 0f)
//            .setBottomLeftCorner(CustomCornerTreatment())
//            .setTopRightCorner(CornerFamily.ROUNDED, 0f)
//            .build()

//        homeBinding.cardView.shapeAppearanceModel = homeBinding.cardView.shapeAppearanceModel.toBuilder()
//            .setTopLeftCorner(CustomCornerTreatment())
//            .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
//            .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
//            .build()
        return homeBinding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() : HomeFragment {
            return HomeFragment()
        }
    }
}