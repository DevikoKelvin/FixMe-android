package com.erela.fixme.objects

data class NewNotificationResponse(
	val latestNotif: String? = null,
	val count: Int? = null,
	val notifications: List<NotificationsItem?>? = null
)

data class NotificationsItem(
	val idUser2: String? = null,
	val idUser1: String? = null,
	val idNotif: String? = null,
	val tglWaktuBaca: String? = null,
	val stsBaca: String? = null,
	val actions: String? = null,
	val tglWaktu: String? = null
)

