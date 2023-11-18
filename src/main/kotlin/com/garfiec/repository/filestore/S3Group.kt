package com.garfiec.repository.filestore

enum class FileBuckets(val displayName: String, val isPublic: Boolean) {
    SOULSAVOR_PUBLIC("soulsavor-public", true),
    SOULSAVOR_INTERNAL("soulsavor", false)
}

enum class ObjectPrefix(val displayName: String) {
    MERCHANT_GROUP_PHOTO("merchant-group-photo"),
    USER_PHOTO("user-photo"),
    DISH_PHOTO("dish-photo")
}

// soulsavor/merchant-group-photo/de97f659-d461-449c-bb79-48fad74b8f2a.jpg
// soulsavor/user-photo/93d0ebfb-0bfd-476b-97eb-9e71ac02c648.jpg
