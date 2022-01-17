package com.evolveasia.cloudstorageutil.utils

import android.graphics.Bitmap
import org.json.JSONObject

internal fun getAwsMetaData(
    image: ImageMetaData,
    awsConfig: AwsMetaInfo.AWSConfig,
    addWatermark: Boolean
): AwsMetaInfo {
    val imageExtraParams = JSONObject()
    imageExtraParams.put("location", "${image.getLat()},${image.getLng()}")
    return AwsMetaInfo.Builder().apply {
        this.serviceConfig = awsConfig
        this.awsFolderPath = image.getCloudUrl()
        this.imageMetaInfo = AwsMetaInfo.ImageMetaInfo().apply {
            this.imagePath = image.getPathOfImage()
            this.mediaType = AwsMetaInfo.ImageMetaInfo.TYPE_JPEG
            this.metadata = imageExtraParams.toString()
            this.compressLevel = 80
            this.imageWidth = 1080
            this.imageHeight = 720
            this.compressFormat = Bitmap.CompressFormat.JPEG
            if (addWatermark) {
                val waterMarkInfo = mutableListOf<Pair<String, String>>()
                if (image.getLat() > 0 && image.getLng() > 0) {
                    waterMarkInfo.add(
                        Pair(
                            "lat_lng",
                            "Lat-Lng ${image.getLat()},${image.getLng()}"
                        )
                    )
                }
                waterMarkInfo.add(Pair("time_stamp", "Capture Time: ${image.getTimeStamp()}"))
                waterMarkInfo.add(Pair("outlet_name", "Outlet Name: ${image.getOutletName()}"))
                this.waterMarkInfo = AwsMetaInfo.WaterMarkInfo(waterMarkInfo)
            }
        }
    }.build()
}