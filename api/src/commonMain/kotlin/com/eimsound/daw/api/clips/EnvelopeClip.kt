package com.eimsound.daw.api.clips

import com.eimsound.daw.api.controllers.ParameterController
import com.eimsound.dsp.data.EnvelopePointList

/**
 * @see com.eimsound.daw.impl.clips.envelope.EnvelopeClipImpl
 */
interface EnvelopeClip : Clip {
    val envelope: EnvelopePointList
    val controllers: MutableList<ParameterController>
}

/**
 * @see com.eimsound.daw.impl.clips.envelope.EnvelopeClipFactoryImpl
 */
interface EnvelopeClipFactory: ClipFactory<EnvelopeClip>
