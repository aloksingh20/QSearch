package com.example.yourassistant

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel:ViewModel() {

    private val _uiState : MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState.Initial)
    val uiState  = _uiState.asStateFlow()

    private lateinit var generateModelVision : GenerativeModel

    private lateinit var generateModelText : GenerativeModel

    init {

        val config = generationConfig {
            temperature = 0.70f
        }

        generateModelVision = GenerativeModel(
            modelName = "gemini-pro-vision",
            apiKey = com.example.yourassistant.BuildConfig.apiKey,
            generationConfig = config
        )
        generateModelText = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = com.example.yourassistant.BuildConfig.apiKey,
            generationConfig  =config
        )
    }
    fun askQuestion( userInput:String, selectedImages : List<Bitmap> ){
        _uiState.value = HomeUiState.Loading
        val prompt = "$userInput"
        viewModelScope.launch (Dispatchers.IO){
            try {
                var output=""
                if(selectedImages.isEmpty()){
                    val response = generateModelText.generateContent(prompt)

                        output += response.text.toString()
                        _uiState.value = HomeUiState.Success(response.text.toString())


                }else {

                    val content = content {
                        for (bitmap in selectedImages) {
                            image(bitmap)
                            text(prompt)
                        }
                    }

                   val response= generateModelVision.generateContent(content)
                    output += response.text
                    _uiState.value = HomeUiState.Success(output)


                }

            }catch (e:Exception){
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Error in generating the response")

            }
        }
    }
}


sealed interface HomeUiState{

    object Initial : HomeUiState
    object Loading : HomeUiState
    data class Success(
        var outPutText:String
    ) : HomeUiState
    data class Error(
        var error:String
    ) : HomeUiState

}