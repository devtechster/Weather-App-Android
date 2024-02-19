package com.adhiraj.weather.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.getInstance
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

import com.adhiraj.weather.model.WeatherResponse
import com.adhiraj.weather.ui.theme.WeatherZipTheme
import com.adhiraj.weather.viewmodel.WeatherViewModel
import com.adhiraj.weatherzip.R
import kotlinx.coroutines.delay
import java.util.LinkedList
import java.util.Queue

//TODO - Store recent searches (numberqueue) in the ROOM database
private lateinit var weatherViewModel: WeatherViewModel
private var zipCodeQueue: Queue<String> = LinkedList()

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            weatherViewModel = ViewModelProvider(
                this,
                getInstance(application)
            )[WeatherViewModel::class.java]

            zipCodeQueue = weatherViewModel.numberQueue
            weatherViewModel.addHardcodedZipCodesInQueue()

            WeatherZipTheme {
                ScaffoldExample()
            }
        }
    }


}


@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldExample() {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(stringResource(id = R.string.app_name))
                }
            )
        },
    )
    { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            ZipSearchBar()


        }
    }
}

//Main search bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipSearchBar() {
    var text by rememberSaveable { mutableStateOf("") }
    var selectedZipCode by rememberSaveable { mutableStateOf("02119") }
    var active by rememberSaveable { mutableStateOf(false) }
    var weatherResponseDataToSave by rememberSaveable { mutableStateOf<WeatherResponse?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .testTag("SEARCH_BAR_TEST_TAG")

                .semantics {
                    traversalIndex = -1f
                },
            query = text,
            onQueryChange = {
                text = it
                selectedZipCode = it
            },
            onSearch = {
                active = false
            },
            active = active,
            onActiveChange = {
                active = it
                selectedZipCode = ""
            },
            placeholder = { Text(stringResource(id = R.string.default_search_text)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (active) {
                    Icon(Icons.Default.Clear, contentDescription = null,
                        modifier = Modifier.clickable {
                            text = ""
                            selectedZipCode = ""
                        })
                }
            },
        ) {

            for (zipcode in zipCodeQueue.reversed()) {

                if (zipcode.contains(text)) {
                    ListItem(
                        headlineContent = { Text(zipcode) },
                        leadingContent = {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .clickable {
                                text = zipcode
                                selectedZipCode = zipcode
                                active = false

                            }
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

            }
        }


        if (!active) {
            if (text.trim().isNotEmpty()) {
                if (zipCodeQueue.isNotEmpty()) {

                    if (!zipCodeQueue.contains(selectedZipCode)) {

                        zipCodeQueue.add(selectedZipCode)
                        if (zipCodeQueue.size > 4)
                            zipCodeQueue.remove()
                    } else {

                        zipCodeQueue.add(selectedZipCode)
                        zipCodeQueue.remove(selectedZipCode)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(
                        PaddingValues(
                            start = 32.dp,
                            top = 16.dp,
                            end = 32.dp,
                            bottom = 16.dp
                        ),
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                var loadingState by remember { mutableStateOf("Loading") }
                var errorText by remember { mutableStateOf("") }
                LaunchedEffect(selectedZipCode) {
                    weatherResponseDataToSave = null
                    weatherViewModel.weatherResponse = null
                    weatherViewModel.getWeatherDataFromAPI(selectedZipCode)
                        ?.doOnNext { weatherResponse ->
                            if (weatherResponse != null) {

                                weatherResponseDataToSave = weatherResponse
                            }
                            loadingState = "Done"
                        }?.subscribe(
                            {
                                loadingState = "Done"
                                weatherResponseDataToSave = it
                            }, {
                                errorText = it.localizedMessage!!
                                loadingState = "Done"
                                weatherResponseDataToSave = null
                            }

                        )

//                        delay(2000)
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                ) {
                    Row(

                        verticalAlignment = Alignment.CenterVertically
                    ) { /*...*/ }
                    Spacer(Modifier.size(56.dp))
                    var isVisible by remember { mutableStateOf(true) }

                    LaunchedEffect(isVisible) {

                        if (isVisible) {

                            // Show for 1 seconds
                            delay(1000)

                            // After 1 seconds, hide the content
                            isVisible = false
                        }
                    }

                    if (isVisible || loadingState == "Loading") {
                        LoadingIcon()
                    } else if (text.trim().isEmpty() || !isVisible) {
                        if (weatherResponseDataToSave == null) {
                            NoZipcodeFoundText(errorText)
                        } else
                            NewLocationCard(zIPCode = selectedZipCode, weatherResponseDataToSave)

                    }
                    WeatherMonitoringAnimation()
                }
            }


        }


    }
}


@Composable
fun NoZipcodeFoundText(error: String) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = error,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Left,
        )

    }

}

@Composable
fun EnterZipcodeText() {

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.empty_search_error),

            modifier = Modifier
                .fillMaxWidth()
                .testTag("ENTER_ZIPCODE_TEXT_WIDGET")
                .padding(16.dp),
            textAlign = TextAlign.Center,
        )

    }

}

@Composable
fun NewLocationCard(zIPCode: String, weatherResponse: WeatherResponse?) {
    if (weatherResponse != null) {
        val responseCity =
            weatherResponse?.places?.get(0)?.observations?.get(0)?.place?.address?.city ?: ""
        val responseState =
            weatherResponse?.places?.get(0)?.observations?.get(0)?.place?.address?.state ?: ""
        val responseTempDesc =
            weatherResponse?.places?.get(0)?.observations?.get(0)?.temperatureDesc ?: ""
        val responseTemp = weatherResponse?.places?.get(0)?.observations?.get(0)?.temperature ?: ""
        val weatherImageId: Int
        when (responseTempDesc) {
            "Extremely Cold" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Frigid" -> {
                weatherImageId = R.drawable.wsymbol_0002_sunny_intervals
            }

            "Cold" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Chilly" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Nippy" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Cool" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Extremely" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Refreshing Cool" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Mild" -> {
                weatherImageId = R.drawable.wsymbol_0002_sunny_intervals
            }

            "Pleasantly Warm" -> {
                weatherImageId = R.drawable.wsymbol_0002_sunny_intervals
            }

            "Warm" -> {
                weatherImageId = R.drawable.wsymbol_0002_sunny_intervals
            }

            "Hot" -> {
                weatherImageId = R.drawable.wsymbol_0002_sunny_intervals
            }

            "Extremely Hot" -> {
                weatherImageId = R.drawable.wsymbol_0001_sunny
            }

            "Windy" -> {
                weatherImageId = R.drawable.wsymbol_0007_fog
            }

            "Gusty" -> {
                weatherImageId = R.drawable.wsymbol_0017_cloudy_with_light_rain
            }

            "Blustery" -> {
                weatherImageId = R.drawable.wsymbol_0017_cloudy_with_light_rain
            }

            "Very Windy" -> {
                weatherImageId = R.drawable.wsymbol_0024_thunderstorms
            }

            "Extremely Windy" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "High Winds" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Beautiful" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Very Nice" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Nice" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Dreary" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Smoggy" -> {
                weatherImageId = R.drawable.wsymbol_0004_black_low_cloud
            }

            "Low Level Pollution" -> {
                weatherImageId = R.drawable.wsymbol_0006_mist
            }

            "Blowing Snow" -> {
                weatherImageId = R.drawable.wsymbol_0011_light_snow_showers
            }

            "Blowing Spray" -> {
                weatherImageId = R.drawable.wsymbol_0009_light_rain_showers
            }

            "Dry" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Comfortable" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Crisp" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Raw" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Damp" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Humid" -> {
                weatherImageId = R.drawable.wsymbol_0010_heavy_rain_showers
            }

            "Muggy" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Sultry" -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }

            "Steamy" -> {
                weatherImageId = R.drawable.wsymbol_0003_white_cloud
            }

            else -> {
                weatherImageId = R.drawable.wsymbol_0021_cloudy_with_sleet
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier
                .testTag("NEW_LOCATION_CARD_WIDGET")
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,

                ) {
                Column {
                    Modifier.weight(2f)
                    Text(
                        text = "ZIP: $zIPCode\nCity: $responseCity\nState: $responseState\n$responseTempDesc $responseTemp°C",
                        modifier = Modifier
                            .padding(16.dp),
                        textAlign = TextAlign.Left,
                    )
                }
                Column {
                    Modifier.weight(1f)
                    val imageModifier = Modifier
                        .size(120.dp)

                    Image(
                        painter = painterResource(id = weatherImageId),
                        contentDescription = responseTempDesc,
                        contentScale = ContentScale.Fit,
                        modifier = imageModifier
                    )
                }


            }


        }
    }
}

@Composable
fun LoadingIcon() {

    Column(

    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.surfaceVariant,
            trackColor = MaterialTheme.colorScheme.secondary,
        )

    }
}

@Composable
fun WeatherMonitoringAnimation() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.businessteam))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
}