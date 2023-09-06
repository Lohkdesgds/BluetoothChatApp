package com.lsw.nearbychat

class MMessage {
    lateinit var ath: String
    lateinit var msg: String

    constructor(cath: String, cmsg: String)
    {
        ath = cath
        msg = cmsg
    }
    constructor(combo: String) {
        fromString(combo)
    }

    override fun toString() : String {
        return ath + "\n" + msg
    }

    fun fromString(combo: String) {
        ath = combo.substring(0, combo.indexOf('\n'))
        msg = combo.substring(combo.indexOf('\n') + 1)
    }
}