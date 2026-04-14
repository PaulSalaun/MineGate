package com.saunaltech.mindgate.app.data.remote

import com.saunaltech.mindgate.BuildConfig

object SupabaseConfig {
    const val BASE_URL: String = BuildConfig.SUPABASE_URL
    const val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    val AUTHORIZATION: String get() = "Bearer $ANON_KEY"
}