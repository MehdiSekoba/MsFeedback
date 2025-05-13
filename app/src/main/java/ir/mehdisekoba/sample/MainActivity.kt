package ir.mehdisekoba.sample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import ir.mehdisekoba.feedback.MsFeedback
import ir.mehdisekoba.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    //binding
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            btnSend.setOnClickListener {
                MsFeedback.Builder(this@MainActivity)
                    .withEmail("webmahdi72@gmail.com")
                    .withSystemInfo()
                    .build()
                    .start()
            }
        }

    }
}