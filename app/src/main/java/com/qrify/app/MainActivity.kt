package com.qrify.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.apexhub.sdk.ApexHubUpdater
import com.apexhub.sdk.ApkInstaller
import com.qrify.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentBitmap: Bitmap? = null
    private var lastEncoded: String? = null

    private val updater: ApexHubUpdater by lazy {
        (application as QrifyApp).updater
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.generateButton.setOnClickListener { generate() }
        binding.shareButton.setOnClickListener { shareQr() }
        binding.urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                generate(); true
            } else false
        }

        // ApexHub: check for an update on launch → dialog → download → install.
        lifecycleScope.launch {
            try {
                updater.checkAndPrompt(activity = this@MainActivity)
            } catch (_: Throwable) {
                // Never let an update-check failure crash the app.
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume a pending install if the user left to grant the
        // "install unknown apps" permission and came back.
        try {
            ApkInstaller.resumePendingInstall(this)
        } catch (_: Throwable) {
        }
    }

    private fun generate() {
        val raw = binding.urlInput.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) {
            binding.urlLayout.error = getString(R.string.error_empty)
            return
        }
        binding.urlLayout.error = null
        val content = normalizeUrl(raw)
        hideKeyboard()

        binding.progress.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            val bmp = runCatching {
                withContext(Dispatchers.Default) { QrGenerator.encode(content) }
            }
            binding.progress.visibility = android.view.View.GONE
            bmp.onSuccess {
                currentBitmap = it
                lastEncoded = content
                binding.qrImage.setImageBitmap(it)
                binding.qrImage.contentDescription = getString(R.string.qr_for, content)
                binding.encodedText.text = content
                binding.shareButton.isEnabled = true
                binding.resultGroup.visibility = android.view.View.VISIBLE
            }.onFailure {
                Toast.makeText(this@MainActivity, R.string.error_generate, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Prepend https:// if the user typed a bare host like "example.com". */
    private fun normalizeUrl(input: String): String {
        val hasScheme = input.contains("://") ||
            input.startsWith("mailto:", true) ||
            input.startsWith("tel:", true)
        val looksLikeHost = input.contains('.') && !input.contains(' ')
        return if (!hasScheme && looksLikeHost) "https://$input" else input
    }

    private fun shareQr() {
        val bmp = currentBitmap ?: return
        lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                val dir = File(cacheDir, "qr").apply { mkdirs() }
                val file = File(dir, "qrify.png")
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(
                    this@MainActivity,
                    "$packageName.fileprovider",
                    file
                )
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri as Uri)
                lastEncoded?.let { putExtra(Intent.EXTRA_TEXT, it) }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_qr)))
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.urlInput.windowToken, 0)
    }
}
