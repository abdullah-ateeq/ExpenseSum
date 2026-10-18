package com.example.expensesum.data

interface KeyValueStore {
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String?)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    fun remove(key: String)
    fun clear()
}
