package com.erela.fixme.objects

data class UserData(
    val id: Int,
    val idStarConnect: Int,
    val username: String,
    val name: String,
    val privilege: Int,
    val idDept: Int,
    val dept: String,
    val subDept: String,
    val email: String = "",
    /**
     * Does this person WORK the laundry counter — which decides which Smart Wash screen opens.
     *
     * GA split check-in on 12 Sep 2026: the courier scans the counter, the counter operator scans
     * the garments. True opens the operator's queue, false the courier's arrival button.
     *
     * NOT the web's access level, and that is deliberate. `LaundryAccess::MANAGE` includes every
     * super user from any department, which is right for "may you open the management pages" and
     * wrong here — `hendrick`, `chandra` and `devikokelvin` are IT and hand uniforms in like
     * anybody else. The server answers the counter question separately with `isCounterStaff()`.
     *
     * Sent as the DECISION rather than the inputs, as `ac_access` already is: it depends on a
     * config list and a sub-department name, so a client re-deriving it would drift from the
     * server the first time either changed. The AC menu in MainActivity re-derives its own gate
     * from hardcoded department strings, and that is the mistake this avoids.
     *
     * Defaults to false so a client holding a login from before this field existed behaves as a
     * courier rather than opening a queue it cannot fill.
     */
    val isLaundryCounter: Boolean = false
)