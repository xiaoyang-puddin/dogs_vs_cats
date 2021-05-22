package com.example.demo0201_dogs_vs_cats

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.icu.text.AlphabeticIndex
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class classifier (assetManager: AssetManager, modelPath: String){
    private var interpreter: Interpreter
    private var imageSizeX:Int
    private var imageSizeY:Int
    private var pixelSize:Int

    data class Recognition(
        var id: String = "",
        var title: String = "",
        var confidence: Float = 0F
    ) {
        override fun toString(): String {
            return "Title = $title, Confidence = $confidence"
        }
    }


    init {
        val tfliteOptions = Interpreter.Options()
        tfliteOptions.setNumThreads(5)
        tfliteOptions.setUseNNAPI(true)
        interpreter = Interpreter(loadModelFile(assetManager, modelPath),tfliteOptions)
        Log.i(this::class.simpleName,"\n=====模型加载成功=====")

        //获取输入信息
        val imageTensorIndex=0
        val imageShape:IntArray= interpreter.getInputTensor(imageTensorIndex).shape()//{1,224,224,3
        val inputDataType=interpreter.getInputTensor(imageTensorIndex).dataType()
         imageSizeY=imageShape[1]
         imageSizeX=imageShape[2]
         pixelSize=imageShape[3]

        Log.i(this::class.simpleName,"\n=====图片的宽"+imageSizeX.toString())
        Log.i(this::class.simpleName,"\n=====图片的高"+imageSizeY.toString())
        Log.i(this::class.simpleName,"\n=====图片的色彩通道"+pixelSize.toString())
        Log.i(this::class.simpleName,"\n=====输入的数据类型"+inputDataType.toString())

        //获取输出信息
        val probabilityTensorIndex=0
        val probabilityShape=interpreter.getOutputTensor(imageTensorIndex).shape()//{1,classes}
        var outputDataType=interpreter.getOutputTensor(probabilityTensorIndex).dataType()
        Log.i(this::class.simpleName,"\n=====分类的数量"+probabilityShape[1])
        Log.i(this::class.simpleName,"\n=====图片的色彩通道"+outputDataType.toString())

    }
    fun recognizeImage(bitmap: Bitmap):Recognition{
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSizeX ,imageSizeY, false)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)
        Log.i(this::class.simpleName,"\n=====图片转换成功=====")

        val result = Array(1) {FloatArray(1)}
        interpreter.run(byteBuffer, result)
        Log.i(this::class.simpleName,"\n=====推断的结果:"+result[0][0])

        return getResult(result)
    }



    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(imageSizeX * imageSizeY * pixelSize*4)
        imgData.order(ByteOrder.nativeOrder())
        imgData.rewind()

        val intValues = IntArray(imageSizeX *imageSizeY)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until imageSizeX) {
            for (j in 0 until imageSizeY) {
                val value = intValues[pixel++]

                imgData.putFloat((value.shr(16)and 0xFF) /255.0f)
                imgData.putFloat((value.shr(16)and 0xFF) /255.0f)
                imgData.putFloat((value and 0xFF) /255.0f)
            }
        }
        return imgData
    }
    private fun getResult(labelProbArray:Array<FloatArray>):Recognition {
        var recognition:Recognition? = null
        if(labelProbArray[0][0]>=0.5){
            recognition=Recognition("1","狗",labelProbArray[0][0])
            }
        else{
            recognition=Recognition("0","猫",1-labelProbArray[0][0])
        }
        return  recognition

    }

}