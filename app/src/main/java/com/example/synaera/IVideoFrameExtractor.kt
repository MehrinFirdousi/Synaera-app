package com.example.synaera

import java.nio.ByteBuffer

interface IVideoFrameExtractor {
    fun onCurrentFrameExtracted(currentFrame: Frame)
    fun onAllFrameExtracted(processedFrameCount: Int, processedTimeMs: Long)
}