package io.github.soldierinwhite.rcricketplace

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.github.soldierinwhite.rcricketplace.ui.theme.RCricketPlaceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val INTERVAL = 5000L
    private val placeAndMotifBitmapLiveData: MutableLiveData<Pair<Bitmap, Bitmap>> =
        MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loader = ImageLoader(this)
        val placeRequest = ImageRequest.Builder(this)
            .data("https://imgur.com/6b2TA4K.png")
            .allowHardware(false) // Disable hardware bitmaps.
            .build()
        val motifRequest = ImageRequest.Builder(this)
            .data("https://i.imgur.com/Cpvqfne.png")
            .allowHardware(false)
            .build()
        lifecycleScope.launch {
            while (true) {
                val placeBitmap =
                    ((loader.execute(placeRequest) as? SuccessResult)?.drawable as BitmapDrawable).bitmap
                val motifBitmap =
                    ((loader.execute(motifRequest) as? SuccessResult)?.drawable as BitmapDrawable).bitmap
                placeAndMotifBitmapLiveData.postValue(placeBitmap.run {
                    copy(config, true)
                } to motifBitmap.run {
                    copy(config, true)
                })
                delay(INTERVAL)
            }
        }


        lifecycleScope.launch {
            while (true) {
                delay(INTERVAL)
            }
        }
        setContent {
            RCricketPlaceTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ShowRandomPixelToChange(
                        placeAndMotifBitmapLiveData = placeAndMotifBitmapLiveData
                    )
                }
            }
        }
    }
}

@Composable
fun ShowRandomPixelToChange(
    placeAndMotifBitmapLiveData: LiveData<Pair<Bitmap, Bitmap>>
) {
    val placeAndMotifBitmap = placeAndMotifBitmapLiveData.observeAsState().value
    placeAndMotifBitmap?.let { placeAndMotif ->
        val (place, motif) = placeAndMotif
        val pixelLocationsToColors = (0 until motif.width).mapNotNull { x ->
            (0 until motif.height).mapNotNull { y ->
                if (motif.getPixel(x, y) != Color.Transparent.toArgb()) {
                    (x to y) to java.lang.String.format(
                        "#%06X",
                        0xFFFFFF and motif.getPixel(x, y)
                    )
                } else null
            }
        }.flatten()
        Column {
            val randomWrongPixel =
                pixelLocationsToColors.filter { (coordinate, color) ->
                    val (x, y) = coordinate
                    java.lang.String.format(
                        "#%06X",
                        0xFFFFFF and place.getPixel(x, y)
                    ) != color
                }.randomOrNull()

            Image(
                bitmap = place.asImageBitmap(),
                contentDescription = "Place",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 32.dp)
            )
            Text(
                if (randomWrongPixel != null) "Change pixel at x=${randomWrongPixel.first.first} and y=${randomWrongPixel.first.second} to color=${randomWrongPixel.second}" else "Our image is exactly as it should be, well done!",
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(top = 32.dp)
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
