package myedu.oshsu.kg

/**
 * Centralized constants for the MyEDU application.
 * No values should be hardcoded across the codebase — use these constants instead.
 */
object AppConstants {

    // --- SharedPreferences ---
    const val PREFS_NAME = "myedu_offline_cache"

    // --- Preference Keys: Auth ---
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_REMEMBER_ME = "pref_remember_me"
    const val KEY_SAVED_EMAIL = "pref_saved_email"
    const val KEY_SAVED_PASS = "pref_saved_pass"

    // --- Preference Keys: Settings ---
    const val KEY_THEME_MODE = "theme_mode_pref"
    const val KEY_DOC_DOWNLOAD_MODE = "doc_download_mode"
    const val KEY_LANGUAGE = "language_pref"
    const val KEY_SHOW_WIDGET_PROMOTION = "show_widget_promotion"
    const val KEY_CUSTOM_NAME = "local_custom_name"
    const val KEY_CUSTOM_PHOTO = "local_custom_photo"
    const val KEY_CUSTOM_DICTIONARY = "custom_dictionary_json"

    // --- Preference Keys: Cached Data ---
    const val KEY_USER_DATA = "user_data"
    const val KEY_PROFILE_DATA = "profile_data"
    const val KEY_PAY_STATUS = "pay_status"
    const val KEY_VERIFY_2FA = "verify_2fa_status"
    const val KEY_NEWS_LIST = "news_list"
    const val KEY_SCHEDULE_LIST = "schedule_list"
    const val KEY_SESSION_LIST = "session_list"
    const val KEY_TRANSCRIPT_LIST = "transcript_list"
    const val KEY_TIME_MAP = "time_map"

    // --- App States ---
    const val STATE_STARTUP = "STARTUP"
    const val STATE_LOGIN = "LOGIN"
    const val STATE_APP = "APP"

    // --- Theme Modes ---
    const val THEME_SYSTEM = "SYSTEM"
    const val THEME_LIGHT = "LIGHT"
    const val THEME_DARK = "DARK"
    const val THEME_GLASS = "GLASS"
    const val THEME_GLASS_DARK = "GLASS_DARK"

    // --- Splash Screen ---
    const val SPLASH_MIN_DURATION_MS = 1100L

    // --- Download Modes ---
    const val DOC_MODE_IN_APP = "IN_APP"
    const val DOC_MODE_WEBSITE = "WEBSITE"

    // --- Network ---
    const val API_BASE_URL = "https://api3.myedu.oshsu.kg/"
    const val GITHUB_API_BASE_URL = "https://api.github.com/"
    const val PORTAL_BASE_URL = "https://myedu.oshsu.kg"
    const val PORTAL_BASE_URL_SLASH = "https://myedu.oshsu.kg/"
    const val PRIMARY_API_HOST = "api3.myedu.oshsu.kg"
    const val BACKUP_API_HOST = "api.myedu.oshsu.kg"
    const val COOKIE_DOMAIN = "myedu.oshsu.kg"

    // --- Cookie Names ---
    const val COOKIE_JWT = "myedu-jwt-token"
    const val COOKIE_LAST_UPDATE = "my_edu_update"
    const val COOKIE_2FA = "have_2fa"
    const val COOKIE_2FA_VALUE = "yes"
    const val COOKIE_PATH = "/"

    // --- HTTP Headers ---
    const val HEADER_USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
    const val HEADER_ACCEPT_VALUE = "application/json, text/plain, */*"

    // --- Timeouts (seconds) ---
    const val API_CONNECT_TIMEOUT_SEC = 60L
    const val API_READ_TIMEOUT_SEC = 60L
    const val GITHUB_CONNECT_TIMEOUT_SEC = 30L
    const val GITHUB_READ_TIMEOUT_SEC = 30L

    // --- Intervals ---
    const val REFRESH_COOLDOWN_MINUTES = 5L
    const val BACKGROUND_SYNC_HOURS = 4L
    const val DEBUG_TRIGGER_HOLD_MS = 3000L
    const val DEBUG_LOG_MAX_ENTRIES = 1000
    const val UPDATE_POLL_INTERVAL_MS = 500L

    // --- Cached Files ---
    const val AVATAR_CACHE_FILENAME = "cached_avatar"

    // --- Database ---
    const val DATABASE_NAME = "myedu_database"

    // --- WorkManager ---
    const val WORK_SYNC_NAME = "MyEduGradeSync"

    // --- Notification ---
    const val NOTIFICATION_CHANNEL_ID = "myedu_notif_channel"
    const val NOTIFICATION_CHANNEL_DESC = "Class alerts"
    val NOTIFICATION_VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500)

    // --- Intent Extras ---
    const val EXTRA_TITLE = "TITLE"
    const val EXTRA_MESSAGE = "MESSAGE"
    const val EXTRA_ID = "ID"

    // --- Email ---
    const val EMAIL_DOMAIN = "@oshsu.kg"

    // --- PDF Libraries ---
    const val PDFMAKE_JS_URL = "https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/pdfmake.min.js"
    const val PDFMAKE_FONTS_URL = "https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/vfs_fonts.js"

    // --- MIME Types ---
    const val MIME_APK = "application/vnd.android.package-archive"
    const val MIME_PDF = "application/pdf"
    const val MIME_PLAIN_TEXT = "text/plain"

    // --- Subject Types (for matching) ---
    const val SUBJECT_LECTION_EN = "Lection"
    const val SUBJECT_PRACTICAL_EN = "Practical"
    const val SUBJECT_LAB_EN = "Lab"
    const val SUBJECT_LECTURE_RU = "Лекция"
    const val SUBJECT_PRACTICAL_RU = "Практические"
    const val SUBJECT_LAB_RU = "Лаборатор"
}
