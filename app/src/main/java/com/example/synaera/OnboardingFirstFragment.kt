package com.example.synaera

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.synaera.databinding.FragmentOnboardingFirstBinding

class OnboardingFirstFragment : Fragment() {

    lateinit var binding: FragmentOnboardingFirstBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() : OnboardingFirstFragment {
            return OnboardingFirstFragment()
            }
    }
}