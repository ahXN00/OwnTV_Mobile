package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileListRow

/**
 * Who is watching. Tapping a profile switches to it, which is the whole of what this page does —
 * creating, editing and PIN-locking one belong to the profile gate, and arrive with it.
 */
@Composable
fun SettingsProfilePage(
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val activeId = vm.settings.activeProfileId.pref(-1L)

    SettingsPage(modifier) {
        settingsSection(R.string.profiles_title) {
            profiles.forEach { profile ->
                MobileListRow(
                    title = profile.name,
                    subtitle = when {
                        profile.isKids -> stringResource(R.string.profiles_kids_tag)
                        profile.pinHash != null -> stringResource(R.string.profiles_locked_tag)
                        else -> null
                    },
                    leading = { Icon(Icons.Filled.Person, contentDescription = null) },
                    onClick = { vm.edit { setActiveProfile(profile.id) } },
                    trailing = if (profile.id == activeId) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
