package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class NewNotificationResponse(
	@SerializedName("latest_notif")
	val latestNotif: String? = null,
	val count: Int? = null,
	val notifications: List<NotificationsItem?>? = null
)

data class NotificationsItem(
	@SerializedName("id_user_2")
	val idUser2: Int? = null,
	@SerializedName("id_user_1")
	val idUser1: Int? = null,
	@SerializedName("id_notif")
	val idNotif: Int? = null,
	@SerializedName("tgl_waktu_baca")
	val tglWaktuBaca: String? = null,
	@SerializedName("sts_baca")
	val stsBaca: Int? = null,
	val actions: String? = null,
	@SerializedName("tgl_waktu")
	val tglWaktu: String? = null,
	@SerializedName("id_gaprojects")
	val caseId: Int? = null,
)

