package com.firebase.io2024.whoami

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.firebase.io2024.whoami.ui.theme.WhoAmITheme
import com.google.firebase.Firebase
import com.google.firebase.vertexai.type.generationConfig
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
  private val deviceInfo = MutableStateFlow("Device details information here...!!")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      WhoAmITheme {
        val info by deviceInfo.collectAsState()
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
          Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp)) // Add space between button and text
            Button(onClick = { promptAi { newInfo -> deviceInfo.value = newInfo.toString() } }) {
              Text(
                "Current Device",
                modifier = Modifier.width(IntrinsicSize.Max),
                textAlign = TextAlign.Center,
              )
            }

            Spacer(modifier = Modifier.height(32.dp)) // Add space between button and text
            Text(
              text = info,
              modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(32.dp)) // Add space between button and text
            Button(onClick = { parseContent() }) {
              Text(
                "Get Device Name",
                modifier = Modifier.width(IntrinsicSize.Max),
                textAlign = TextAlign.Center,
              )
            }
            Spacer(modifier = Modifier.height(16.dp)) // Add space between button and text
            Button(onClick = { openCamera() }) {
              Text(
                "Open Camera - Who Am I?",
                modifier = Modifier.width(IntrinsicSize.Max),
                textAlign = TextAlign.Center,
              )
            }
            Spacer(modifier = Modifier.height(16.dp)) // Add space between button and text
          }
        }
      }
    }
  }

  private fun parseContent() {
    var jsonText = deviceInfo.value
    if (jsonText.startsWith("```json") && jsonText.endsWith("```")) {
      // The json is wrapped in a code markup block
      jsonText = jsonText.substring(7, jsonText.length - 3).trim()
    }
    val jsonObject = JSONObject(jsonText)
    deviceInfo.value = jsonObject.getString("name")
  }

  private fun openCamera() {
    if (!Build.MANUFACTURER.equals("Google")) {
      val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      startCameraLauncher.launch(cameraIntent)
    } else {
      val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
      val cameraId = cameraManager.cameraIdList[0]
      val cameraDevice = cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
          Log.i("TAG", "Camera successfully opened")
        }

        override fun onDisconnected(camera: CameraDevice) {
          Log.i("TAG", "Camera successfully disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
          Log.i("TAG", "Camera had an error, retry!")
        }
      }, null)
    }

  }

  private val startCameraLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == RESULT_OK) {
      // Image captured successfully, handle the image data here
      val imageUri = result.data?.data ?: return@registerForActivityResult
    }
  }

  private fun promptAi(callback: (Any?) -> Unit) =
    lifecycleScope.launch {
      val generativeModel =
        Firebase.vertexAI.generativeModel(
          modelName =
          if (Build.MANUFACTURER != "google") "gemini-1.0-pro-002" else "gemini-1.0-pro",
          generationConfig = generationConfig { temperature = 0.7f },
        )

      deviceInfo.value = "Fetching device details"
      val manufacturer = Build.MANUFACTURER
      val deviceName = Build.MODEL// Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
      val deviceDetailsPrompt = """Give me details about the phone $manufacturer $deviceName as a 
        | JSON format without adding any extra details. Include information about the features that
        | are available on the device including camera, bluetooth, size, weight and battery. Respond
        | as just a plain JSON text without including any markups
        |like quotes or back quotes.""".trimMargin()
      val result = generativeModel.generateContent(deviceDetailsPrompt)

      Log.i("TAG", "find me Prompt: ${deviceDetailsPrompt}")
      Log.i("TAG", "find me Response: ${result.text}")
      callback(result.text.toString())
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Button(
    onClick = {
      val generativeModel =
        Firebase.vertexAI.generativeModel(
          modelName = "gemini-1.0-pro",
          generationConfig = generationConfig { temperature = 0.7f },
        )
    }
  ) {
    Text(text = "Hello $name!", modifier = modifier)
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  WhoAmITheme { Greeting("Android") }
}
