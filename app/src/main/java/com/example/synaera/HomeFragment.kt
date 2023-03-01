package com.example.synaera

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.synaera.databinding.FragmentHomeBinding
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.CornerTreatment

class HomeFragment : Fragment() {

    private lateinit var homeBinding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeBinding = FragmentHomeBinding.inflate(inflater, container, false)
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