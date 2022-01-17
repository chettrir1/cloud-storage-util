package com.evolveasia.cloudstorageutil

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtilityOptions
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.evolveasia.cloudstorageutil.utils.*
import java.io.*

class AWSUtils(
    private val awsMetaInfo: AwsMetaInfo,
    private val context: Context,
    val onAwsImageUploadListener: OnAwsImageUploadListener,
) {
    private var imageFile: File? = null
    private var mTransferUtility: TransferUtility? = null
    private var sS3Client: AmazonS3Client? = null
    private var sCredProvider: CognitoCachingCredentialsProvider? = null

    private fun getCredProvider(context: Context): CognitoCachingCredentialsProvider? {
        if (sCredProvider == null) {
            sCredProvider = CognitoCachingCredentialsProvider(
                context.applicationContext,
                awsMetaInfo.serviceConfig.cognitoPoolId,
                getRegions(awsMetaInfo)
            )
        }
        return sCredProvider
    }

    private fun getS3Client(context: Context?): AmazonS3Client? {
        if (sS3Client == null) {
            sS3Client =
                AmazonS3Client(
                    getCredProvider(context!!),
                    Region.getRegion(awsMetaInfo.serviceConfig.region)
                )
        }
        return sS3Client
    }

    private fun getTransferUtility(context: Context): TransferUtility? {
        if (mTransferUtility == null) {
            val tuOptions = TransferUtilityOptions()
            tuOptions.transferThreadPoolSize = 10 // 10 threads for upload and download operations.

            // Initializes TransferUtility
            mTransferUtility = TransferUtility
                .builder()
                .s3Client(getS3Client(context.applicationContext))
                .context(context.applicationContext)
                .transferUtilityOptions(tuOptions)
                .build()
        }
        return mTransferUtility
    }

    fun beginUpload() {
        if (TextUtils.isEmpty(awsMetaInfo.imageMetaInfo.imagePath)) {
            onAwsImageUploadListener.onError("Could not find the filepath of the selected file")
            return
        }

        val compressedBitmap = compressAwsImage(awsMetaInfo).second
        if (compressedBitmap != null) {
            val newBitmap = Bitmap.createBitmap(
                compressedBitmap,
                0,
                0,
                compressedBitmap.width,
                compressedBitmap.height
            )
            if (newBitmap != null) {
                // newBitmap will be recycled inside addAwsWaterMark function
                val waterMarkBitmap = addAwsWaterMark(awsMetaInfo, newBitmap)
                waterMarkBitmap.recycle()
            }
            compressedBitmap.recycle()
        }

        val file = File(awsMetaInfo.imageMetaInfo.imagePath)
        imageFile = file
        onAwsImageUploadListener.showProgress()

        try {
            val observer = getTransferUtility(context)?.upload(
                awsMetaInfo.serviceConfig.bucketName, //Bucket name
                "${awsMetaInfo.awsFolderPath}/${imageFile?.name}", imageFile
            )
            observer?.setTransferListener(UploadListener())
        } catch (e: Exception) {
            e.printStackTrace()
            onAwsImageUploadListener.onError(e.message.toString())
        }
    }

    private inner class UploadListener : TransferListener {
        override fun onError(id: Int, e: Exception) {
            onAwsImageUploadListener.onError(e.message.toString())
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            onAwsImageUploadListener.onProgressChanged(
                id,
                bytesCurrent.toFloat(),
                bytesTotal.toFloat()
            )
        }

        override fun onStateChanged(id: Int, newState: TransferState) {
            if (newState == TransferState.COMPLETED) {
                val finalImageUrl =
                    "${awsMetaInfo.serviceConfig.url}${awsMetaInfo.awsFolderPath}/${imageFile?.name}"
                onAwsImageUploadListener.onSuccess(finalImageUrl)
            } else if (newState == TransferState.CANCELED || newState == TransferState.FAILED) {
                onAwsImageUploadListener.onError("Error in uploading file.")
            }
        }
    }

    private fun compressAwsImage(awsMetaInfo: AwsMetaInfo): Pair<String, Bitmap?> {
        return try {
            val byteArray = streamToByteArray(FileInputStream(awsMetaInfo.imageMetaInfo.imagePath))
            val bitmap = decodeSampledBitmapFromResource(
                byteArray, awsMetaInfo.imageMetaInfo.imageWidth
                    ?: AwsConstant.DEFAULT_IMAGE_WIDTH, awsMetaInfo.imageMetaInfo.imageHeight
                    ?: AwsConstant.DEFAULT_IMAGE_HEIGHT, awsMetaInfo.imageMetaInfo.waterMarkInfo
            )

            val stream = ByteArrayOutputStream()
            bitmap.compress(
                awsMetaInfo.imageMetaInfo.compressFormat,
                awsMetaInfo.imageMetaInfo.compressLevel,
                stream
            )
            val os: OutputStream = FileOutputStream(awsMetaInfo.imageMetaInfo.imagePath)
            os.write(stream.toByteArray())
            os.close()
            Pair(awsMetaInfo.imageMetaInfo.imagePath, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(awsMetaInfo.imageMetaInfo.imagePath, null)
        }
    }

    private fun getRegions(awsMetaInfo: AwsMetaInfo): Regions {
        return when (awsMetaInfo.serviceConfig.region) {
            "ap-southeast-1" -> Regions.AP_SOUTHEAST_1
            "ap-south-1" -> Regions.AP_SOUTH_1
            "ap-east-1" -> Regions.AP_EAST_1
            else -> throw IllegalArgumentException("Invalid region : add other region if required (Cloud storage util library)")
        }
    }

    interface OnAwsImageUploadListener {
        fun showProgress()
        fun onProgressChanged(id: Int, currentByte: Float, totalByte: Float)
        fun onSuccess(imgUrl: String)
        fun onError(errorMsg: String)
    }
}