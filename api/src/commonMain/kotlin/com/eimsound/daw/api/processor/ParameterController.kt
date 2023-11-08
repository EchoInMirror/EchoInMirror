package com.eimsound.daw.api.processor

import com.eimsound.audioprocessor.IAudioProcessorParameter
import com.eimsound.daw.commons.json.JsonSerializable

interface ParameterController : JsonSerializable {
    val parameters: List<IAudioProcessorParameter>
}
