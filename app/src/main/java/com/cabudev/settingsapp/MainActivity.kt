package com.cabudev.settingsapp

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cabudev.settingsapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.datastore: DataStore<Preferences> by preferencesDataStore(name = "settings")



class MainActivity : AppCompatActivity() {

    companion object{
        const val VOLUME_LVL = "volume_lvl"
        const val KEY_BLUETOOTH = "key_bluetooth"
        const val KEY_DARKMODE = "key_darkmode"
        const val KEY_VIBRATION = "key_vibration"
    }

    private lateinit var binding: ActivityMainBinding
    var firstTime:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        CoroutineScope(Dispatchers.IO).launch {
            getSettings().filter { firstTime }.collect{ SettingsModel ->
                //data
                if(SettingsModel != null){
                    runOnUiThread {
                        binding.sDarkMode.isChecked = SettingsModel.darkMode
                        binding.sBlueTooth.isChecked = SettingsModel.bluetooh
                        binding.sVibration.isChecked = SettingsModel.vibration
                        binding.rsVolume.setValues(SettingsModel.volume.toFloat())
                    }
                    firstTime = !firstTime
                }
            }
        }
        initUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initUI() {
        binding.rsVolume.addOnChangeListener { _, value, _ ->
            CoroutineScope(Dispatchers.IO).launch {
                saveVolume(value.toInt())
            }
        }
        binding.sBlueTooth.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch{
                saveOptions(KEY_BLUETOOTH, isChecked)
            }
        }

        binding.sVibration.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch{
                saveOptions(KEY_VIBRATION, isChecked)
            }
        }

        binding.sDarkMode.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.IO).launch{
                saveOptions(KEY_DARKMODE, isChecked)
            }
        }
    }

    private suspend fun saveVolume(volume:Int){
        datastore.edit { preference ->
            preference[intPreferencesKey(VOLUME_LVL)] = volume
        }
    }

    private suspend fun saveOptions(key:String, value:Boolean){
        datastore.edit { preference -> 
            preference[booleanPreferencesKey(key)] = value 
        }
    }

    private fun getSettings(): Flow<SettingsModel?> {
        return datastore.data.map { preferences ->
            SettingsModel(
                volume = preferences[intPreferencesKey(VOLUME_LVL)]?:50,
                darkMode = preferences[booleanPreferencesKey(KEY_DARKMODE)] ?: false,
                vibration = preferences[booleanPreferencesKey(KEY_VIBRATION)] ?: true,
                bluetooh = preferences[booleanPreferencesKey(KEY_BLUETOOTH)] ?: false
            )

        }
    }
}