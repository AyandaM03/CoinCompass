package com.example.coincompass

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.coincompass.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Properly find the NavController from the FragmentContainerView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Setup Bottom Navigation with NavController using explicit NavigationUI call
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        // Add a listener to ensure the Home button (and others) are always responsive
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Use onNavDestinationSelected to handle the standard navigation logic
            // This also handles popping up to the start destination correctly.
            val handled = NavigationUI.onNavDestinationSelected(item, navController)
            
            // Return true to update the selected item in the UI
            handled
        }

        // Handle re-selection (e.g. scroll to top)
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_home) {
                // If already on Home and clicked Home again, ensure it's at the start
                navController.popBackStack(R.id.nav_home, false)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
