package com.example.coincompass.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.coincompass.R
import com.example.coincompass.databinding.FragmentMoreBinding
import com.example.coincompass.ui.HistoryActivity
import com.example.coincompass.ui.RewardsActivity
import com.example.coincompass.ui.AddCategoryActivity

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnMoreCalendar.setOnClickListener {
            safeNavigate(R.id.nav_calendar)
        }

        binding.btnMoreHistory.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }

        binding.btnMoreRewards.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsActivity::class.java))
        }

        binding.btnMoreCategories.setOnClickListener {
            startActivity(Intent(requireContext(), AddCategoryActivity::class.java))
        }

        binding.btnMoreSavings.setOnClickListener {
            safeNavigate(R.id.nav_savings)
        }
    }

    private fun safeNavigate(destinationId: Int) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.nav_more) {
            navController.navigate(destinationId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
