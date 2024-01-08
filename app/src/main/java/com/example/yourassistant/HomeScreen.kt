package com.example.yourassistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieDrawable.RepeatMode
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.yourassistant.ui.theme.fontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun AppContent(voiceToTextParser:VoiceToTextParser,viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {

    val appUiState = viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val imageRequestBuilder = ImageRequest.Builder(LocalContext.current)

    val imageLoader = ImageLoader.Builder(LocalContext.current).build()

    HomeScreen( uiState = appUiState.value, LocalContext.current, voiceToTextParser ){inputText, selectedItems ->

        coroutineScope.launch {
            val bitmaps = selectedItems.mapNotNull {
                val imageRequest = imageRequestBuilder
                    .data(it)
                    .size(786)
                    .build()

                val imageResult = imageLoader.execute(imageRequest)
                if(imageResult is SuccessResult){
                    return@mapNotNull (imageResult.drawable as BitmapDrawable).bitmap
                }else{
                    return@mapNotNull null
                }
            }

            viewModel.askQuestion(userInput = inputText, selectedImages = bitmaps)
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(uiState: HomeUiState = HomeUiState.Loading,context: Context, voiceToTextParser: VoiceToTextParser,
               onSendClicked: (String, List<Uri>) -> Unit) {
    var userQues by rememberSaveable { mutableStateOf("") }
    val imageUris = rememberSaveable(saver = UriCustomSaver()) { mutableStateListOf() }
    val conversationItems = rememberSaveable(saver = ConversationSaver()) { mutableStateListOf<ConversationItem>() }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.welcome_animation))

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { imageUri ->
        imageUri?.let {
            imageUris.add(it)
        }
    }

    var canRecord by remember{
        mutableStateOf(false)
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {isGranted ->
            canRecord = isGranted
        })

    LaunchedEffect(key1 = recordAudioLauncher){
        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }



//    LaunchedEffect(Unit) {
//        composition. // Start the animation
//    }

    val imageCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { result: Boolean ->
        result?.let {
            // The picture was taken successfully, handle the captured image URI (result)
        } ?: run {
            // Handle the case where taking a picture was unsuccessful
        }
    }

    var isSendingMessage by remember { mutableStateOf(false) }
    val state by voiceToTextParser.state.collectAsState()
//    LaunchedEffect(imageUris) {
//        Log.d("MyApp", "ImageUris changed: $imageUris")
//    }

    LaunchedEffect(key1 = state.spokenText) {
        // Check if the spoken text is not empty and append it to userQues
        if (state.spokenText.isNotEmpty()) {
            userQues += " " + state.spokenText
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "QSearch") },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (conversationItems.isNotEmpty()) {


                        IconButton(onClick = { conversationItems.clear() }) {
                            Icon(
                                imageVector = Icons.Rounded.ClearAll,
                                contentDescription = "",
                                tint = Color.Black
                            )
                        }
                    }
                }


            )

        },
        bottomBar = {
            Column {
                Card (
                    modifier = Modifier.padding(6.dp)
                ){
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add Image",
                                modifier = Modifier.padding(0.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (state.isSpeaking) {
                                    voiceToTextParser.stopListening()
                                } else {
                                    voiceToTextParser.startListening()
                                }
                            },
                        ) {
                            AnimatedContent(targetState = state.isSpeaking, label = "") {
                                if (it) {
                                    Icon(
                                        modifier = Modifier.padding(0.dp),
                                        tint = Color.Red,
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = "Take Picture"
                                    )

                                } else {
                                    Icon(
                                        modifier = Modifier.padding(0.dp),
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = "Take Picture"
                                    )

                                }
                            }

                        }

                        OutlinedTextField(

                            value = userQues,
                            onValueChange = {
                                userQues = it
                            },
                            placeholder = { Text(text = "Ask question") },
                            modifier = Modifier.fillMaxWidth(0.83f),
                            shape = OutlinedTextFieldDefaults.shape,
                        )

                        IconButton(onClick = {
                            if (userQues.isNotBlank() && userQues.isNotEmpty()) {
                                isSendingMessage = true
                                conversationItems.add(ConversationItem(userQues, true))
                                onSendClicked(userQues, imageUris)

                            }
                        }) {
                            if (isSendingMessage) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = imageUris.size > 0) {
                    Card(modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()) {
                        LazyRow(modifier = Modifier.padding(8.dp)) {
                            items(imageUris) { imageUri ->
                                Column {
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .requiredSize(50.dp)
                                    )
                                    TextButton(onClick = { imageUris.remove(imageUri) }) {
                                        Text(text = "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        when (uiState) {
            is HomeUiState.Initial -> {
            }
            is HomeUiState.Loading -> {
                isSendingMessage = true
            }
            is HomeUiState.Success -> {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)

                    isSendingMessage = false
                    if(uiState.outPutText.isNotEmpty()&& userQues.isNotEmpty()){
                        conversationItems.add(ConversationItem(uiState.outPutText, false))
                        userQues = ""
                        uiState.outPutText=""
                    }
                    uiState.outPutText=""
                }

            }
            is HomeUiState.Error -> {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)


                    isSendingMessage = false
                    if (uiState.error.isNotEmpty() && userQues.isNotEmpty()) {
                        conversationItems.add(ConversationItem(uiState.error, false))
                        userQues = ""
                        uiState.error = ""
                    }
                    uiState.error = ""
                }
            }
        }

       AnimatedVisibility(visible = conversationItems.isNotEmpty()) {
//           Box(
//               modifier = Modifier
//                   .fillMaxSize().fillMaxWidth()
//
//           ) {
//               Row (verticalAlignment = Alignment.CenterVertically){
//                   Text(text = "Search history",
//                       textAlign = TextAlign.Left)
//                   IconButton(
//                       onClick = {
//                           // Handle the clear action
//                           conversationItems.clear()
//                       }
//                   ) {
//                       Icon(
//                           modifier = Modifier.size(24.dp),
//                           imageVector = Icons.Rounded.ClearAll, contentDescription = "")
//                   }
//               }
//           }
           Column(
               modifier = Modifier
                   .fillMaxSize()
                   .padding(it)
                   .padding(8.dp)
                   .verticalScroll(rememberScrollState())
           ) {

               Box(modifier = Modifier.weight(1f)) {

                   LazyColumn {
                       items(conversationItems) { item ->
                           Row(verticalAlignment = Alignment.CenterVertically) {
                               val iconResId = if (item.isUserMessage) R.drawable.icon_person else R.drawable.assistant
                               val painter = painterResource(id = iconResId)

                               Image(
                                   painter = painter,
                                   contentDescription = "Custom Icon",
                                   modifier = Modifier.size(24.dp)
                               )
//                            Icon(painter = painterResource(id = icon), contentDescription = "Message Icon", modifier = Modifier.size(24.dp))

                               var isExpanded by remember { mutableStateOf(false) }
                               var isCopying by remember { mutableStateOf(false) }


                               Card(
                                   modifier = Modifier
                                       .padding(8.dp)
                                       .fillMaxSize(),
                                   colors = if (item.isUserMessage) {
                                       CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary)
                                   } else {
                                       CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                   }
                               ) {
                                   Column {
                                       Row {
                                           Text(

                                               modifier = Modifier
                                                   .padding(start = 12.dp, top = 8.dp)
                                                   .weight(0.98f),
                                               text = item.message,
                                               maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                               overflow = TextOverflow.Ellipsis,
                                               color = if (item.isUserMessage) {
                                                   MaterialTheme.colorScheme.primary
                                               } else {
                                                   MaterialTheme.colorScheme.onPrimaryContainer
                                               }
                                           )

                                           IconButton(onClick = {
                                               conversationItems.remove(item)
                                           }) {
                                               Icon(
                                                   modifier = Modifier.size(19.dp),
                                                   imageVector = Icons.Rounded.Delete,
                                                   contentDescription = "delete")
                                           }
                                       }
                                       Row (modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically){
                                           Icon(
                                               modifier = Modifier
                                                   .clickable {
                                                       isExpanded = !isExpanded
                                                   }
                                                   .padding(4.dp)
                                                   .size(24.dp),
                                               imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                               contentDescription = "Expand/Collapse"
                                           )
                                           // Copy option using ClipboardManager
                                           IconButton(
                                               onClick = {
                                                   val clipboardManager =
                                                       context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                   val clipData =
                                                       ClipData.newPlainText("Copied Text", item.message)
                                                   clipboardManager.setPrimaryClip(clipData)

                                                   // Set isCopying to true to change the icon
                                                   isCopying = true

                                                   // Start a coroutine to reset isCopying after 5 seconds
                                                   CoroutineScope(Dispatchers.Main).launch {
                                                       delay(5000)
                                                       isCopying = false
                                                   }
                                               },
                                               modifier = Modifier
                                                   .padding(4.dp)
                                                   .size(16.dp),
                                           ) {
                                               Icon(

                                                   painter = if (isCopying) painterResource(R.drawable.icon_copy_fill) else painterResource(R.drawable.icon_copy),
                                                   contentDescription = "Copy Text"

                                               )
                                           }
                                       }

                                   }

                               }
                           }
                       }
                   }
               }


           }
       }

        AnimatedVisibility(
            visible = conversationItems.isEmpty(),
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Add space above the card

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(16.dp)
            ){

                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(0.5f, false)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(0.dp,50.dp,0.dp,50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Hello",
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            letterSpacing = TextUnit.Unspecified

                            )
                        LottieAnimation(composition = composition,
                            modifier = Modifier.size(84.dp),
                            isPlaying = true,
                            iterations = 5,
                            restartOnPlay = true,
                            renderMode = RenderMode.AUTOMATIC

                        )

                        Text(
                            text = "Unlock the power of \"Q\". ",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(20.dp,0.dp,20.dp,0.dp)
                        )
                        Text(
                            text = "What questions spark your mind? Let's dive in and find the answers together.",
                            textAlign = TextAlign.Center,
                            color = Color.Black,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(20.dp,5.dp,20.dp,0.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )


                    }
                }
            }
        }


    }

}

//
//@Composable
//fun LottieAnimation() {
//    val composotion by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.welcome_animation))
//
//}
data class ConversationItem(val message: String, val isUserMessage: Boolean)

