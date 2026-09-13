package com.example.phoenx.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phoenx.R
import com.example.phoenx.data.local.CreatorProfileEntity
import com.example.phoenx.ui.components.InfoButton
import com.example.phoenx.ui.components.OnboardingPopup
import com.example.phoenx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorRichProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreatorRichProfileViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val theme = LocalAppTheme.current
    val accent = theme.accentColor

    OnboardingPopup(
        pageKey = "rich_profile",
        title = stringResource(R.string.rich_profile_onboarding_title),
        contentPoints = listOf(
            stringResource(R.string.rich_profile_onboarding_point1),
            stringResource(R.string.rich_profile_onboarding_point2)
        ),
        preferenceManager = themeViewModel.preferenceManager
    )

    // On utilise un profil temporaire local pour l'édition en temps réel
    var currentProfile by remember(profile) { mutableStateOf(profile ?: CreatorProfileEntity(userId = "")) }

    // Sections accordéon
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = theme.backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rich_profile_screen_title), color = theme.contentColor, fontFamily = theme.fontFamily) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.contentColor)
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accent, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { viewModel.updateProfile(currentProfile) }) {
                            Text(stringResource(R.string.rich_profile_button_save), color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                    InfoButton(
                        title = stringResource(R.string.rich_profile_info_title),
                        points = listOf(stringResource(R.string.rich_profile_info_content))
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                stringResource(R.string.rich_profile_header_desc),
                style = MaterialTheme.typography.bodySmall,
                color = theme.contentColor.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 1 : QUI JE SUIS
            ProfileAccordionSection(
                title = stringResource(R.string.rich_profile_section_who_am_i),
                icon = Icons.Default.Face,
                isExpanded = expandedSection == "identity",
                onToggle = { expandedSection = if (expandedSection == "identity") null else "identity" },
                accent = accent
            ) {
                OutlinedTextField(
                    value = currentProfile.bio ?: "",
                    onValueChange = { currentProfile = currentProfile.copy(bio = it) },
                    label = { Text(stringResource(R.string.rich_profile_label_bio)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    colors = richProfileTextFieldColors(accent, theme)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = currentProfile.profession ?: "",
                    onValueChange = { currentProfile = currentProfile.copy(profession = it) },
                    label = { Text(stringResource(R.string.rich_profile_label_profession)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = richProfileTextFieldColors(accent, theme)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2 : FAMILLE
            ProfileAccordionSection(
                title = stringResource(R.string.rich_profile_section_family),
                icon = Icons.Default.FamilyRestroom,
                isExpanded = expandedSection == "family",
                onToggle = { expandedSection = if (expandedSection == "family") null else "family" },
                accent = accent
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rich_profile_question_siblings), color = theme.contentColor, modifier = Modifier.weight(1f))
                    BooleanSelector(
                        value = currentProfile.hasSiblings,
                        onValueChange = { currentProfile = currentProfile.copy(hasSiblings = it) },
                        accent = accent
                    )
                }
                if (currentProfile.hasSiblings == true) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentProfile.siblingsDetail ?: "",
                        onValueChange = { currentProfile = currentProfile.copy(siblingsDetail = it) },
                        label = { Text(stringResource(R.string.rich_profile_label_siblings_detail)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = richProfileTextFieldColors(accent, theme)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rich_profile_question_children), color = theme.contentColor, modifier = Modifier.weight(1f))
                    BooleanSelector(
                        value = currentProfile.hasChildren,
                        onValueChange = { currentProfile = currentProfile.copy(hasChildren = it) },
                        accent = accent
                    )
                }
                if (currentProfile.hasChildren == true) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentProfile.childrenDetail ?: "",
                        onValueChange = { currentProfile = currentProfile.copy(childrenDetail = it) },
                        label = { Text(stringResource(R.string.rich_profile_label_children_detail)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = richProfileTextFieldColors(accent, theme)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3 : PORTRAIT PHYSIQUE
            ProfileAccordionSection(
                title = stringResource(R.string.rich_profile_section_physical),
                icon = Icons.Default.AccessibilityNew,
                isExpanded = expandedSection == "physical",
                onToggle = { expandedSection = if (expandedSection == "physical") null else "physical" },
                accent = accent
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentProfile.height?.toString() ?: "",
                        onValueChange = { currentProfile = currentProfile.copy(height = it.toIntOrNull()) },
                        label = { Text(stringResource(R.string.rich_profile_label_height)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = richProfileTextFieldColors(accent, theme)
                    )
                    OutlinedTextField(
                        value = currentProfile.weight?.toString() ?: "",
                        onValueChange = { currentProfile = currentProfile.copy(weight = it.toIntOrNull()) },
                        label = { Text(stringResource(R.string.rich_profile_label_weight)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = richProfileTextFieldColors(accent, theme)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentProfile.eyeColor ?: "",
                        onValueChange = { currentProfile = currentProfile.copy(eyeColor = it) },
                        label = { Text(stringResource(R.string.rich_profile_label_eye_color)) },
                        modifier = Modifier.weight(1f),
                        colors = richProfileTextFieldColors(accent, theme)
                    )
                    OutlinedTextField(
                        value = currentProfile.hairColor ?: "",
                        onValueChange = { currentProfile = currentProfile.copy(hairColor = it) },
                        label = { Text(stringResource(R.string.rich_profile_label_hair_color)) },
                        modifier = Modifier.weight(1f),
                        colors = richProfileTextFieldColors(accent, theme)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 4 : PASSIONS
            ProfileAccordionSection(
                title = stringResource(R.string.rich_profile_section_hobbies),
                icon = Icons.Default.Favorite,
                isExpanded = expandedSection == "hobbies",
                onToggle = { expandedSection = if (expandedSection == "hobbies") null else "hobbies" },
                accent = accent
            ) {
                OutlinedTextField(
                    value = currentProfile.hobbies ?: "",
                    onValueChange = { currentProfile = currentProfile.copy(hobbies = it) },
                    label = { Text(stringResource(R.string.rich_profile_label_hobbies_desc)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    colors = richProfileTextFieldColors(accent, theme)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ProfileAccordionSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, theme.contentColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        color = theme.contentColor.copy(alpha = 0.02f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, color = theme.contentColor, fontWeight = FontWeight.Bold)
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = theme.contentColor.copy(alpha = 0.4f)
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun BooleanSelector(value: Boolean?, onValueChange: (Boolean?) -> Unit, accent: Color) {
    val theme = LocalAppTheme.current
    Row {
        FilterChip(
            selected = value == true,
            onClick = { onValueChange(if (value == true) null else true) },
            label = { Text(stringResource(R.string.rich_profile_boolean_yes)) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = value == false,
            onClick = { onValueChange(if (value == false) null else false) },
            label = { Text(stringResource(R.string.rich_profile_boolean_no)) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent, selectedLabelColor = theme.backgroundColor)
        )
    }
}

@Composable
fun richProfileTextFieldColors(accent: Color, theme: AppThemeState) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    focusedLabelColor = accent,
    focusedTextColor = theme.contentColor,
    unfocusedTextColor = theme.contentColor,
    unfocusedBorderColor = theme.contentColor.copy(alpha = 0.2f)
)
