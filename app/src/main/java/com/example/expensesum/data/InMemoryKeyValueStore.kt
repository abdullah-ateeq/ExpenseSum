package com.example.expensesum.data

class InMemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, Any?>()

    override fun getString(key: String, defaultValue: String?): String? {
        return map[key] as? String ?: defaultValue
    }

    override fun putString(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return map[key] as? Boolean ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        map[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return map[key] as? Int ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        map[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return map[key] as? Long ?: defaultValue
    }

    override fun putLong(key: String, value: Long) {
        map[key] = value
    }

    override fun remove(key: String) {
        map.remove(key)
    }

    override fun clear() {
        map.clear()
    }
}
