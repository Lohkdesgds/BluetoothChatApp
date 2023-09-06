@file:Suppress("DEPRECATION")

package com.lsw.nearbychat

import android.content.SharedPreferences

class MPreferences(val m_pref: SharedPreferences)
{
    private val m_editor = m_pref.edit()

    fun get(key: String, default_value: String): String {
        val gottn = m_pref.getString(key, default_value)
        if (gottn == null) return default_value
        return gottn
    }

    fun set(key: String, value: String) {
        m_editor.putString(key, value)
        m_editor.commit()
    }

    fun getList(key: String): List<String> {
        val gotten: Set<String> = m_pref.getStringSet(key, setOf<String>()) ?: return listOf<String>()

        val sorted = gotten.sortedBy({it.substring(0, it.indexOf('|')).toInt()})
        val cut = arrayListOf<String>()

        sorted.forEach { it -> cut.add(it.substring(it.indexOf('|') + 1)) }

        return cut.toList()
    }

    fun setList(key: String, list: List<String>) {
        var mnew = arrayListOf<String>()

        list.forEachIndexed {
            idx, it -> mnew += (1000000000 + idx).toString() + "|" + it
        }

        m_editor.putStringSet(key, mnew.toSet())
        m_editor.commit()
    }

    fun erase(key: String) {
        m_editor.remove(key)
        m_editor.commit()
    }

    fun clear() {
        m_editor.clear()
        m_editor.commit()
    }
}