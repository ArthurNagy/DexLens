package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import android.content.res.AssetManager
import com.arthurnagy.dexlens.core.domain.scan.PictureScores
import com.arthurnagy.dexlens.core.domain.scan.ReferenceGallery
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer

/**
 * The bundled encoder on LiteRT's CPU runtime, and the reference gallery it is matched against. It needs a device: the
 * runtime is native, so JVM tests use a fake engine instead.
 */
internal class LiteRtPictureEngine(
    private val assets: AssetManager,
) : PictureEngine {
    private var model: CompiledModel? = null
    private var gallery: ReferenceGallery? = null
    private val inputs = mutableListOf<TensorBuffer>()
    private val outputs = mutableListOf<TensorBuffer>()

    override fun load() {
        try {
            val references = assets.open(REFERENCES_ASSET).use { it.readBytes() }
            gallery = checkNotNull(ReferenceGallery.parse(references)) { "unusable $REFERENCES_ASSET" }
            val options = CompiledModel.Options(Accelerator.CPU).apply { cpuOptions = CompiledModel.CpuOptions(numThreads = CPU_THREADS) }
            val compiled = CompiledModel.create(assets, ENCODER_ASSET, options).also { model = it }
            inputs += compiled.createInputBuffers()
            outputs += compiled.createOutputBuffers()
            // The first run is the slowest; doing it now keeps it off the first real frame.
            score(FloatArray(ColourSquare.SIZE * ColourSquare.SIZE * ColourSquare.CHANNELS))
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    override fun score(input: FloatArray): PictureScores {
        val compiled = checkNotNull(model) { "not loaded" }
        inputs[0].writeFloat(input)
        compiled.run(inputs, outputs)
        return checkNotNull(gallery).rank(outputs[0].readFloat())
    }

    override fun close() {
        (inputs + outputs).forEach { it.close() }
        inputs.clear()
        outputs.clear()
        model?.close()
        model = null
    }

    private companion object {
        const val ENCODER_ASSET = "dexlens_encoder.tflite"
        const val REFERENCES_ASSET = "dexlens_references.bin"
        const val CPU_THREADS = 4
    }
}
