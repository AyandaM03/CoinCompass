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

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val currentDest = navController.currentDestination?.id
            if (item.itemId != currentDest) {
                // Perform standard navigation
                NavigationUI.onNavDestinationSelected(item, navController)
            }
            true
        }

        binding.bottomNavigation.setOnItemReselectedListener { item ->
            // If user clicks the current tab again, pop everything back to the tab's root.
            // This ensures they can always "go back" to the main list (e.g. from Savings to More).
            navController.popBackStack(item.itemId, false)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
