package com.blockchain.presentation.backup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.TertiaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Info
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.icons.withBackground
import com.blockchain.componentlib.navigation.ModeBackgroundColor
import com.blockchain.componentlib.navigation.NavigationBar
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.presentation.R
import com.blockchain.presentation.backup.BackupPhraseIntent
import com.blockchain.presentation.backup.viewmodel.BackupPhraseViewModel
import com.blockchain.walletmode.WalletMode

@Composable
fun SkipBackup(viewModel: BackupPhraseViewModel) {
    SkipBackupScreen(
        backOnClick = { viewModel.onIntent(BackupPhraseIntent.EndFlow(isSuccessful = false)) },
        skipOnClick = { viewModel.onIntent(BackupPhraseIntent.SkipBackup) },
        backUpNowOnClick = { viewModel.onIntent(BackupPhraseIntent.GoToPreviousScreen) }
    )
}

@Composable
fun SkipBackupScreen(
    backOnClick: () -> Unit,
    skipOnClick: () -> Unit,
    backUpNowOnClick: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundMuted),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NavigationBar(
            modeColor = ModeBackgroundColor.Override(WalletMode.NON_CUSTODIAL_ONLY),
            title = stringResource(R.string.backup_phrase_title_secure_wallet),
            onBackButtonClick = backOnClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.standard_spacing)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1F))

            SmallTagIcon(
                icon = StackedIcon.SmallTag(
                    main = Icons.Filled.Lock.withBackground(
                        backgroundColor = Color.White,
                        iconSize = 60.dp,
                        backgroundSize = 88.dp
                    ),
                    tag = Icons.Filled.Info.withTint(AppTheme.colors.error)
                ),
                iconBackground = AppTheme.colors.background,
                borderColor = AppTheme.colors.backgroundMuted,
                mainIconSize = 88.dp
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.standard_spacing)))

            Text(
                text = stringResource(R.string.skip_backup_title),
                style = AppTheme.typography.title3,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.title
            )

            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.tiny_spacing)))

            Text(
                text = stringResource(R.string.skip_backup_description),
                style = AppTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.body
            )

            Spacer(modifier = Modifier.weight(2F))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.skip_backup_cta_skip),
                onClick = skipOnClick
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            TertiaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.skip_backup_cta_backup),
                onClick = backUpNowOnClick
            )
        }
    }
}

// ///////////////
// PREVIEWS
// ///////////////

@Preview(showBackground = true)
@Composable
fun PreviewSkipBackupScreen() {
    SkipBackupScreen(backOnClick = {}, skipOnClick = {}, backUpNowOnClick = {})
}
