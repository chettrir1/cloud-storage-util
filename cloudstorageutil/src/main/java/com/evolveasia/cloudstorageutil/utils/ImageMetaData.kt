package com.evolveasia.cloudstorageutil.utils

interface ImageMetaData {
    fun getLat(): Double
    fun getLng(): Double
    fun getCloudUrl(): String
    fun getTimeStamp(): String
    fun getPathOfImage(): String
    fun getOutletName(): String
    fun getImageId(): Int
    fun getImageType(): String
    fun getUsersName(): String
    fun getBuName(): String
}