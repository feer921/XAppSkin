package com.demo.xappskin

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.demo.xappskin.databinding.ActivityMainBinding
import com.feer.xskin.LayoutInflaterImp
import com.feer.xskin.SkinOfViewDecorator
import com.feer.xskin.XSkinManager
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context?) {
        XSkinManager.registerSkinSpirit(newBase)
        super.attachBaseContext(newBase)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        binding.tvBtnChange.setOnClickListener {
            val resName =  "/skin.apk"
            val filePath = cacheDir.absolutePath + resName
            Log.i("info","--> ${File(filePath).exists()}")
            XSkinManager.getManager().loadSkinRes(this,filePath )
        }

        binding.tvBtnReset.setOnClickListener {
            XSkinManager.getManager().resetSkin()
        }
    }
}