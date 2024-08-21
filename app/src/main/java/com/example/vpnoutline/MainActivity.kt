package com.example.vpnoutline

import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.vpnoutline.OutlineVpnService.Companion.HOST
import com.example.vpnoutline.OutlineVpnService.Companion.PORT
import com.example.vpnoutline.OutlineVpnService.Companion.PASSWORD
import com.example.vpnoutline.OutlineVpnService.Companion.METHOD
import java.nio.charset.StandardCharsets



class MainActivity : ComponentActivity() {

    private lateinit var editTextSsUrl: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonExit: Button
    private val viewModel: MainViewModel by viewModels()
    private val vpnPreparation = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> if (result.resultCode == RESULT_OK) viewModel.startVpn(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonSave = findViewById(R.id.buttonConnect)
        editTextSsUrl = findViewById(R.id.editTextSsUrl)
        buttonExit = findViewById(R.id.buttonExit)


        buttonSave.setOnClickListener {
            val ssUrl = editTextSsUrl.text.toString()
            val shadowsocksInfo = parseShadowsocksUrl(ssUrl)

            HOST = shadowsocksInfo.host
            PORT = shadowsocksInfo.port
            PASSWORD = shadowsocksInfo.password
            METHOD = shadowsocksInfo.method

            startVpn()
        }


        buttonExit.setOnClickListener {
            viewModel.stopVpn(this)
        }
    }



    fun startVpn() = VpnService.prepare(this)?.let {
        vpnPreparation.launch(it)
    } ?: viewModel.startVpn(this)


    fun parseShadowsocksUrl(ssUrl: String): ShadowsocksInfo {
        val regex = Regex("ss://(.*?)@(.*):(\\d+)")
        val matchResult = regex.find(ssUrl)
        if (matchResult != null) {
            val groups = matchResult.groupValues
            val encodedInfo = groups[1]
            val decodedInfo = decodeBase64(encodedInfo)
            val parts = decodedInfo.split(":")
            val method = parts[0]
            val password = parts[1]
            val host = groups[2]
            val port = groups[3].toInt()

            return ShadowsocksInfo(method, password, host, port)
        } else {
            throw IllegalArgumentException("Неверный формат ссылки Outline")
        }
    }

    fun decodeBase64(encoded: String): String {
        val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
        return String(decodedBytes, StandardCharsets.UTF_8)
    }

    data class ShadowsocksInfo(val method: String, val password: String, val host: String, val port: Int)

}

