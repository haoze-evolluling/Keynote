package com.haoze.keynote

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import com.haoze.keynote.ui.navigation.AppPage
import com.haoze.keynote.ui.navigation.Screen
import com.haoze.keynote.ui.theme.DarkModeManager
import com.haoze.keynote.ui.theme.DarkModePreference
import com.haoze.keynote.ui.theme.KeyNoteTheme
import com.haoze.keynote.ui.theme.toDarkModePreference
import com.haoze.keynote.util.PreferencesManager

abstract class KeyNotePageActivity : ComponentActivity() {
    protected abstract val screen: Screen

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.app_startup_background)))
        setContent {
            val preferencesManager = remember { PreferencesManager(applicationContext) }
            val darkModePrefValue by preferencesManager.darkModePreference.collectAsState(initial = null)
            val systemInDarkTheme = isSystemInDarkTheme()
            val isDarkMode = darkModePrefValue?.toDarkModePreference()?.let {
                when (it) {
                    DarkModePreference.SYSTEM -> systemInDarkTheme
                    DarkModePreference.LIGHT -> false
                    DarkModePreference.DARK -> true
                }
            } ?: systemInDarkTheme
            val darkModeManager = remember(darkModePrefValue, isDarkMode) {
                DarkModeManager(darkModePrefValue?.toDarkModePreference() ?: if (isDarkMode) DarkModePreference.DARK else DarkModePreference.LIGHT)
            }
            KeyNoteTheme(darkModeManager = darkModeManager) {
                val colors = androidx.compose.material3.MaterialTheme.colorScheme
                SideEffect {
                    window.setBackgroundDrawable(ColorDrawable(colors.background.toArgb()))
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isDarkMode
                        isAppearanceLightNavigationBars = !isDarkMode
                    }
                }
                Surface(Modifier.fillMaxSize(), color = colors.background) {
                    if (darkModePrefValue != null) {
                        AppPage(
                            route = screen.route,
                            noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it >= 0L },
                            tagId = intent.getLongExtra(EXTRA_TAG_ID, -1L).takeIf { it >= 0L },
                            tagName = intent.getStringExtra(EXTRA_TAG_NAME),
                            initialPage = intent.getIntExtra(EXTRA_INITIAL_PAGE, 0),
                            onNavigate = { route, noteId, tagId, tagName, replace ->
                                startActivity(createIntent(this, route, noteId, tagId, tagName))
                                if (replace) finish()
                            },
                            onBack = ::finish
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_TAG_ID = "tag_id"
        const val EXTRA_TAG_NAME = "tag_name"
        const val EXTRA_INITIAL_PAGE = "initial_page"

        fun createIntent(
            context: Context,
            route: String,
            noteId: Long?,
            tagId: Long?,
            tagName: String?,
            initialPage: Int = 0
        ): Intent {
            val activity = when (route) {
                Screen.FeatureHome.route -> MainActivity::class.java
                Screen.Home.route -> NotesActivity::class.java
                Screen.EditNote.route -> EditNoteActivity::class.java
                Screen.Bill.route -> BillActivity::class.java
                Screen.BillStats.route -> BillStatsActivity::class.java
                Screen.AaSplit.route -> AaSplitActivity::class.java
                Screen.Habit.route -> HabitActivity::class.java
                Screen.Settings.route -> SettingsActivity::class.java
                Screen.AiProviderManage.route -> AiProviderManageActivity::class.java
                Screen.Todo.route -> TodoActivity::class.java
                Screen.TagNotes.route -> TagNotesActivity::class.java
                Screen.KnowledgeVault.route -> KnowledgeVaultActivity::class.java
                Screen.DateGroupNotes.route -> DateGroupNotesActivity::class.java
                Screen.Trash.route -> TrashActivity::class.java
                Screen.DataExport.route -> DataExportActivity::class.java
                Screen.Schedule.route -> ScheduleActivity::class.java
                else -> MainActivity::class.java
            }
            return Intent(context, activity).apply {
                noteId?.let { putExtra(EXTRA_NOTE_ID, it) }
                tagId?.let { putExtra(EXTRA_TAG_ID, it) }
                tagName?.let { putExtra(EXTRA_TAG_NAME, it) }
                if (initialPage != 0) putExtra(EXTRA_INITIAL_PAGE, initialPage)
            }
        }
    }
}

class MainActivity : KeyNotePageActivity() { override val screen = Screen.FeatureHome }
class NotesActivity : KeyNotePageActivity() { override val screen = Screen.Home }
class EditNoteActivity : KeyNotePageActivity() { override val screen = Screen.EditNote }
class BillActivity : KeyNotePageActivity() { override val screen = Screen.Bill }
class BillStatsActivity : KeyNotePageActivity() { override val screen = Screen.BillStats }
class AaSplitActivity : KeyNotePageActivity() { override val screen = Screen.AaSplit }
class HabitActivity : KeyNotePageActivity() { override val screen = Screen.Habit }
class SettingsActivity : KeyNotePageActivity() { override val screen = Screen.Settings }
class AiProviderManageActivity : KeyNotePageActivity() { override val screen = Screen.AiProviderManage }
class TodoActivity : KeyNotePageActivity() { override val screen = Screen.Todo }
class TagNotesActivity : KeyNotePageActivity() { override val screen = Screen.TagNotes }
class KnowledgeVaultActivity : KeyNotePageActivity() { override val screen = Screen.KnowledgeVault }
class DateGroupNotesActivity : KeyNotePageActivity() { override val screen = Screen.DateGroupNotes }
class TrashActivity : KeyNotePageActivity() { override val screen = Screen.Trash }
class DataExportActivity : KeyNotePageActivity() { override val screen = Screen.DataExport }
class ScheduleActivity : KeyNotePageActivity() { override val screen = Screen.Schedule }
