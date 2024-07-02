package com.example.ssrip

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        const val PREF_NAME = "SessionPref"
        const val IS_LOGIN = "IsLoggedIn"
        const val KEY_USER_ID = "userId"
        const val KEY_EMAIL = "email"
    }

    fun createLoginSession(userId: String, email: String) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_USER_ID, userId)
        editor.putString(KEY_EMAIL, email)
        editor.apply()
    }

    fun getUserDetails(): HashMap<String, String?> {
        val user = HashMap<String, String?>()
        user[KEY_USER_ID] = prefs.getString(KEY_USER_ID, null)
        user[KEY_EMAIL] = prefs.getString(KEY_EMAIL, null)
        return user
    }

    fun checkLogin(): Boolean {
        return prefs.getBoolean(IS_LOGIN, false) && auth.currentUser != null
    }

    fun logoutUser() {
        editor.clear()
        editor.apply()
        auth.signOut()
    }
}
