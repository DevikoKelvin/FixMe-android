package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class CategoryListResponse(
    @field:SerializedName(value = "code", alternate = ["Code"])
    val code: Int? = null,
    @field:SerializedName(value = "data", alternate = ["Data"])
    val data: List<Category?>? = null,
    @field:SerializedName(value = "message", alternate = ["Message"])
    val message: String? = null
)

data class Category(
    // The backend sometimes returns either snake_case or camelCase keys.
    // Accept both so Gson can map the response reliably.
    @field:SerializedName(
        value = "active_status",
        alternate = ["activeStatus", "active_status "]
    )
    val activeStatus: Boolean? = null,
    @field:SerializedName(
        value = "category_id",
        alternate = ["categoryId", "categoryID", "id_kategori", "id"]
    )
    val categoryId: Int? = null,
    @field:SerializedName(
        value = "category_name",
        alternate = ["categoryName", "nama_kategori", "name"]
    )
    val categoryName: String? = null
)
