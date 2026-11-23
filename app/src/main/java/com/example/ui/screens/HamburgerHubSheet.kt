package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.ui.theme.LocalGlassPalette
import com.google.firebase.auth.FirebaseUser

enum class HubDestination {
    HOME,
    CHECK_REVISION,
    MY_WHY,
    AI,
    LANGUAGE_TOGGLE,
    PROGRESS,
    NOTIFICATIONS,
    LOOK_AND_FEEL,
    BACKUP,
    SECURITY
}

private data class HubTile(
    val dest: HubDestination,
    val icon: ImageVector,
    val label: String,
    val accent: Color,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HamburgerHubSheet(
    strings: AppStrings,
    currentGoogleUser: FirebaseUser?,
    onDismiss: () -> Unit,
    onSelect: (HubDestination) -> Unit
) {
    val glass = LocalGlassPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tiles = listOf(
        HubTile(HubDestination.HOME, Icons.Rounded.Home, strings.home, glass.primaryAccent, "hub_home"),
        HubTile(HubDestination.CHECK_REVISION, Icons.Rounded.CheckCircle, strings.checkRevision, glass.doneColor, "hub_check_revision"),
        HubTile(HubDestination.MY_WHY, Icons.Rounded.Favorite, strings.myWhy, glass.missedColor, "hub_my_why"),
        HubTile(HubDestination.AI, Icons.Rounded.AutoAwesome, strings.aiTitle, glass.secondaryAccent, "hub_ai"),
        HubTile(HubDestination.LANGUAGE_TOGGLE, Icons.Rounded.Translate, "${strings.languageLabel} (${strings.langToggleChip})", glass.primaryAccent, "hub_language"),
        HubTile(HubDestination.PROGRESS, Icons.Rounded.LocalFireDepartment, strings.progressStreak, glass.dueColor, "hub_progress"),
        HubTile(HubDestination.NOTIFICATIONS, Icons.Rounded.NotificationsActive, strings.notificationSettings, glass.dueColor, "hub_notifications"),
        HubTile(HubDestination.LOOK_AND_FEEL, Icons.Rounded.Palette, strings.lookAndFeel, glass.secondaryAccent, "hub_look_feel"),
        HubTile(HubDestination.BACKUP, Icons.Rounded.Backup, strings.backupExport, glass.doneColor, "hub_backup"),
        HubTile(HubDestination.SECURITY, Icons.Rounded.Shield, strings.security, glass.primaryAccent, "hub_security")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glass.glassElevated,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val cloudIdentity = currentGoogleUser?.displayName?.takeIf { it.isNotBlank() }
                ?: currentGoogleUser?.email?.takeIf { it.isNotBlank() }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(HubDestination.BACKUP) }
                    .testTag("hub_cloud_status_pill"),
                shape = RoundedCornerShape(20.dp),
                color = if (currentGoogleUser != null) glass.doneContainer else glass.glassSurface,
                border = BorderStroke(1.dp, glass.glassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = strings.cloudSyncTitle,
                        tint = if (currentGoogleUser != null) glass.doneColor else glass.primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (cloudIdentity != null) {
                            strings.signedInAs(cloudIdentity)
                        } else {
                            "${strings.cloudSyncTitle} • ${strings.signInWithGoogle}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tiles, key = { it.dest.name }) { tile ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(tile.tag)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                onSelect(tile.dest)
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = glass.glassSurface,
                        border = BorderStroke(1.dp, glass.glassBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(tile.accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tile.icon,
                                    contentDescription = tile.label,
                                    tint = tile.accent,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tile.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.inkPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
