package io.github.soldierinwhite.rcricketplace

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.github.soldierinwhite.rcricketplace.ui.theme.RCricketPlaceTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val INTERVAL = 30000L
    private val placeAndMotifBitmapLiveData: MutableLiveData<Pair<Bitmap, Bitmap>> =
        MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetchBitmaps()
        setContent {
            RCricketPlaceTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ShowRandomPixelToChange(
                        placeAndMotifBitmapLiveData = placeAndMotifBitmapLiveData,
                    ) {
                        fetchBitmaps()
                    }
                }
            }
        }
    }

    private fun fetchBitmaps() {
        val ktor = HttpClient(Android) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }

        val loader = ImageLoader(this)
        val motifRequest = ImageRequest.Builder(this)
            .data("https://i.imgur.com/I7ceXRC.png")
            .allowHardware(false)
            .build()
        lifecycleScope.launch {
            val canvasLeftUrl = ktor.get<CanvasCodes>("https://canvas.codes/canvas").canvas_left
            val placeRequest = ImageRequest.Builder(this@MainActivity)
                .data(canvasLeftUrl)
                .allowHardware(false)
                .build()
            val placeBitmap =
                ((loader.execute(placeRequest) as? SuccessResult)?.drawable as BitmapDrawable).bitmap
            val motifBitmap =
                ((loader.execute(motifRequest) as? SuccessResult)?.drawable as BitmapDrawable).bitmap
            placeAndMotifBitmapLiveData.postValue(placeBitmap.run {
                copy(config, true)
            } to motifBitmap.run {
                copy(config, true)
            })
        }
    }
}

@Composable
fun ShowRandomPixelToChange(
    placeAndMotifBitmapLiveData: LiveData<Pair<Bitmap, Bitmap>>,
    onRefresh: () -> Unit
) {
    val placeAndMotifBitmap = placeAndMotifBitmapLiveData.observeAsState().value
    placeAndMotifBitmap?.let { placeAndMotif ->
        val (place, motif) = placeAndMotif
        val pixelLocationsToColors = (0 until motif.width).mapNotNull { x ->
            (0 until motif.height).mapNotNull { y ->
                if (motif.getPixel(x, y) != 0) {
                    (x to y) to motif.getPixel(x, y)
                } else null
            }
        }.flatten()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            val randomWrongPixel =
                pixelLocationsToColors.filter { (coordinate, color) ->
                    val (x, y) = coordinate
                    place.getPixel(x, y) != color
                }.randomOrNull()

            Image(
                bitmap = place.asImageBitmap(),
                contentDescription = "Place",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 32.dp)
            )
            if (randomWrongPixel != null) {
                Row(
                    modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text("To help out, you should target the following pixel in r/place:\n\nChange pixel at x=${randomWrongPixel.first.first} and y=${randomWrongPixel.first.second} to color=",
                    modifier = Modifier
                        .weight(1f)
                    )
                    val colorString = java.lang.String.format(
                        "#%06X",
                        0xFFFFFF and randomWrongPixel.second
                    )
                    Spacer(
                        modifier = Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .background(color = Color(android.graphics.Color.parseColor(colorString)))
                            .padding(16.dp)
                            .align(alignment = Alignment.Bottom)
                    )
                }
            } else {
                Text("The image is looking good right now!")
            }
            TextButton(
                onClick = onRefresh, modifier = Modifier
                    .padding(top = 32.dp)
                    .align(alignment = Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Refresh for latest r/place state and new coordinates",
                    color = colorResource(id = R.color.black),
                    fontSize = 20.sp,
                    modifier = Modifier
                        .background(
                            color = colorResource(id = R.color.white),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(16.dp)
                )
            }
            Text(
                text = "What we are trying to do is place the image below onto the r/place image you see at the top here",
                modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp),
                color = colorResource(id = R.color.white)
            )
            Image(
                bitmap = motif.asImageBitmap(),
                contentDescription = "Motif",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 32.dp)
            )
        }
    }
}

data class CanvasCodes(
    val ok: Boolean,
    val url: String,
    val canvas_left: String,
    val canvas_right: String,
    val ts: Long
)
