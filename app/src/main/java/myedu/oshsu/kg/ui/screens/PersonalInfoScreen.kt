package myedu.oshsu.kg.ui.screens

import android.graphics.Matrix
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import coil.compose.AsyncImage
import coil.request.ImageRequest
import myedu.oshsu.kg.IdDefinitions
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.R
import myedu.oshsu.kg.UserData
import myedu.oshsu.kg.StudentInfoResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Utility to check if a value is valid
private fun isValid(value: String?): Boolean {
    val s = value?.trim()
    return !s.isNullOrEmpty() && 
           !s.equals("null", true) && 
           s != "-" && 
           s != "Unknown" && 
           s != "0" && 
           s != "ID: -" &&
           s != "ID: 0" &&
           !s.startsWith("- (ID:")
}

private class PersonalRotatingShape(
    private val polygon: RoundedPolygon,
    private val rotation: Float
) : Shape {
    private val matrix = Matrix()
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: LayoutDirection, density: Density): Outline {
        matrix.reset()
        matrix.postScale(size.width / 2f, size.height / 2f)
        matrix.postTranslate(size.width / 2f, size.height / 2f)
        matrix.postRotate(rotation, size.width / 2f, size.height / 2f)
        val androidPath = polygon.toPath()
        androidPath.transform(matrix)
        return Outline.Generic(androidPath.asComposePath())
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun InfoCard(glassmorphismEnabled: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val cardColor = if (glassmorphismEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val cardModifier = if (glassmorphismEnabled) {
        Modifier.fillMaxWidth().then(Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape))
    } else {
        Modifier.fillMaxWidth()
    }
    Card(
        modifier = cardModifier, 
        colors = CardDefaults.cardColors(containerColor = cardColor), 
        shape = shape, 
        elevation = CardDefaults.cardElevation(if (glassmorphismEnabled) 0.dp else 0.dp)
    ) { Column(Modifier.padding(16.dp)) { content() } }
}

@Composable
fun ExpandableCard(glassmorphismEnabled: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val cardColor = if (glassmorphismEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val cardModifier = if (glassmorphismEnabled) {
        Modifier.fillMaxWidth().clickable { expanded = !expanded }.then(Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), shape))
    } else {
        Modifier.fillMaxWidth().clickable { expanded = !expanded }
    }
    Card(
        modifier = cardModifier, 
        colors = CardDefaults.cardColors(containerColor = cardColor), 
        shape = shape, 
        elevation = CardDefaults.cardElevation(if (glassmorphismEnabled) 0.dp else 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.personal_view_details), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Icon(if(expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 16.dp)) { content() }
            }
        }
    }
}

@Composable
fun DataRow(icon: ImageVector?, label: String, value: String?) {
    if (!isValid(value)) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.width(40.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(text = value!!.trim(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
fun MetaDataRow(label: String, value: Any?) {
    val displayValue = value?.toString()?.trim()
    if (!isValid(displayValue)) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = displayValue!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

// --- MAIN SCREEN COMPOSABLE ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(vm: MainViewModel, onClose: () -> Unit) {
    val currentLang = vm.language
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var localUser by remember { mutableStateOf<UserData?>(null) }
    var localProfile by remember { mutableStateOf<StudentInfoResponse?>(null) }
    var isFetching by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        delay(500)
        try {
            val (u, p) = vm.getFreshPersonalInfo()
            localUser = u
            localProfile = p
            isError = false
        } catch (e: Exception) {
            isError = true
            errorMessage = e.message ?: "Unknown Error"
        } finally {
            isFetching = false
        }
    }

    val isLoading = (isFetching || !vm.areDictionariesLoaded) && !isError
    val user = localUser
    val profile = localProfile
    val pds = profile?.pdsstudentinfo
    val mov = profile?.studentMovement
    val military = profile?.pdsstudentmilitary

    val apiFullName = listOfNotNull(user?.last_name, user?.name, user?.father_name).joinToString(" ").ifBlank { "-" }
    val studentId = pds?.id_student?.toString() ?: user?.id?.toString() ?: "-"
    val profileStatus = if (user?.is_pds_approval == true) stringResource(R.string.status_approved) else stringResource(R.string.status_pending)
    
    val cookiePolygon = remember { RoundedPolygon.star(12, innerRadius = 0.8f, rounding = CornerRounding(0.2f)) }
    val infiniteTransition = rememberInfiniteTransition(label = "profile_rot")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart))
    val animatedShape = remember(rotation) { PersonalRotatingShape(cookiePolygon, rotation) }

    // --- DICTIONARY LOOKUPS (ROBUST CHECK) ---
    
    val genderName = pds?.id_male?.let { id -> 
        (IdDefinitions.genders.values.find { it.idIntegration == id } ?: IdDefinitions.genders[id])?.getName(currentLang) 
    }
    val genderDisplay = genderName ?: pds?.id_male?.takeIf { it != 0 }?.let { stringResource(R.string.fmt_id_val, it) }

    // Country uses idIntegrationCitizenship
    val citizenshipName = pds?.id_citizenship?.let { id -> 
        (IdDefinitions.countries.values.find { it.idIntegrationCitizenship == id } ?: IdDefinitions.countries[id])?.getName(currentLang) 
    }
    val citizenshipDisplay = citizenshipName ?: pds?.id_citizenship?.takeIf { it != 0 }?.let { stringResource(R.string.fmt_id_val, it) }

    // Nationality uses idIntegration
    val nationalityName = pds?.id_nationality?.let { id -> 
        (IdDefinitions.nationalities.values.find { it.idIntegration == id } ?: IdDefinitions.nationalities[id])?.getName(currentLang) 
    }
    val nationalityDisplay = nationalityName ?: pds?.id_nationality?.takeIf { it != 0 }?.let { stringResource(R.string.fmt_id_val, it) }

    val specObj = mov?.speciality; val facObj = mov?.faculty ?: specObj?.faculty
    val eduObj = mov?.edu_form; val payObj = mov?.payment_form
    val periodObj = mov?.period; val movTypeObj = mov?.movement_info; val langObj = mov?.language

    val facultyName = facObj?.getName(currentLang) ?: "-"
    val specialityName = specObj?.getName(currentLang) ?: "-"
    val eduFormName = eduObj?.getName(currentLang) ?: "-"
    val payFormName = payObj?.getName(currentLang) ?: "-"
    val periodName = periodObj?.getName(currentLang) ?: IdDefinitions.getPeriodName(mov?.id_period, currentLang)

    val facultyShort = facObj?.getShortName(currentLang)
    val specShort = specObj?.getShortName(currentLang)
    val eduFormShort = eduObj?.getShortName(currentLang)
    val langShort = langObj?.getShortName(currentLang)
    
    // --- PRE-CALCULATE VALIDITY FOR SECTIONS ---
    val hasGeography = isValid(pds?.address) || isValid(pds?.birth_address) || isValid(pds?.residence_address)
    val hasFamily = isValid(pds?.father_full_name) || isValid(pds?.mother_full_name)
    val hasMilitary = military != null && (isValid(military.name) || isValid(military.name_military) || isValid(military.serial_number))
    val hasMovement = isValid(mov?.info_description) || isValid(mov?.info) || isValid(mov?.date_movement)
    val hasSystem = user != null 

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.personal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.desc_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(48.dp)) }
            } else if (isError) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.personal_error_load), style = MaterialTheme.typography.titleMedium)
                    if (errorMessage != null) Text(errorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { isFetching = true; isError = false; scope.launch { try { val (u, p) = vm.getFreshPersonalInfo(); localUser = u; localProfile = p; isError = false } catch (e: Exception) { isError = true; errorMessage = e.message } finally { isFetching = false } } }) { Text(stringResource(R.string.personal_retry)) }
                }
            } else {
                AnimatedVisibility(visible = true, enter = fadeIn(tween(500))) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(bottom = 32.dp)) {
                        
                        // --- HEADER ---
                        item {
                            Spacer(Modifier.height(16.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(136.dp)) {
                                    Box(modifier = Modifier.fillMaxSize().clip(animatedShape)) {
                                        val apiPhoto = profile?.avatar
                                        key(apiPhoto, vm.avatarRefreshTrigger) { AsyncImage(model = ImageRequest.Builder(context).data(apiPhoto).crossfade(true).setParameter("retry_hash", vm.avatarRefreshTrigger).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(apiFullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(user?.email ?: "-", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.fmt_id_val, studentId)) }, icon = { Icon(Icons.Default.Badge, null, Modifier.size(16.dp)) })
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // --- 1. IDENTITY & BIO ---
                        item {
                            SectionHeader(stringResource(R.string.personal_identity_bio), Icons.Default.Face)
                            InfoCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                DataRow(Icons.Default.Cake, stringResource(R.string.birthday), pds?.birthday)
                                DataRow(Icons.Default.Face, stringResource(R.string.gender), genderDisplay)
                                DataRow(Icons.Default.Phone, stringResource(R.string.personal_phone), pds?.phone)
                                DataRow(Icons.Default.Phone, stringResource(R.string.personal_residence_phone), pds?.residence_phone)
                                DataRow(Icons.Default.Email, stringResource(R.string.personal_alt_email), user?.email2)
                                DataRow(Icons.Default.Info, stringResource(R.string.personal_profile_status), profileStatus)
                                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                DataRow(Icons.Default.FamilyRestroom, stringResource(R.string.personal_marital_status), pds?.marital_status?.takeIf { it != 0 }?.toString())
                                DataRow(Icons.Default.CheckCircle, stringResource(R.string.personal_is_ethnic), pds?.is_ethnic?.toString())
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // --- 2. ACADEMIC STATUS ---
                        item {
                            SectionHeader(stringResource(R.string.personal_academic_status), Icons.Outlined.School)
                            InfoCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                DataRow(Icons.Outlined.Apartment, stringResource(R.string.faculty), facultyName)
                                DataRow(Icons.Default.Info, stringResource(R.string.personal_faculty_short), facultyShort)
                                if (!facObj?.getInfo(currentLang).isNullOrBlank()) {
                                    DataRow(Icons.Default.Info, stringResource(R.string.personal_faculty_info), facObj?.getInfo(currentLang))
                                }
                                
                                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                DataRow(Icons.Outlined.Class, stringResource(R.string.speciality), specialityName)
                                DataRow(Icons.Default.Info, stringResource(R.string.personal_spec_short), specShort)
                                DataRow(Icons.Default.Info, stringResource(R.string.personal_spec_code), specObj?.code ?: mov?.id_speciality?.takeIf { it != 0 }?.toString())
                                if (!specObj?.getInfo(currentLang).isNullOrBlank()) {
                                    DataRow(Icons.Default.Info, stringResource(R.string.personal_spec_info), specObj?.getInfo(currentLang))
                                }

                                DataRow(Icons.Outlined.Groups, stringResource(R.string.personal_group), mov?.avn_group_name)
                                DataRow(Icons.Default.DateRange, stringResource(R.string.personal_enrollment), mov?.date_movement)
                                
                                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                
                                DataRow(Icons.Default.School, stringResource(R.string.personal_edu_form), eduFormName)
                                DataRow(Icons.Default.Info, stringResource(R.string.personal_edu_short), eduFormShort)
                                DataRow(Icons.Default.CheckCircle, stringResource(R.string.personal_active_status), eduObj?.status?.toString())
                                
                                DataRow(Icons.Outlined.Payments, stringResource(R.string.personal_payment), payFormName)
                                DataRow(Icons.Default.Translate, stringResource(R.string.personal_language), mov?.language?.get(currentLang))
                                DataRow(Icons.Default.Translate, stringResource(R.string.personal_lang_short), langShort)
                                DataRow(Icons.Default.DateRange, stringResource(R.string.personal_current_semester), profile?.active_semester?.takeIf { it != 0 }?.toString())
                                
                                if (!profile?.active_semesters.isNullOrEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.personal_active_semesters_list), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    profile?.active_semesters?.forEach { sem ->
                                        val sName = sem.getName(currentLang)
                                        val displayName = if (sName.isNotBlank()) stringResource(R.string.fmt_name_id, sName, sem.id ?: "-") else stringResource(R.string.fmt_id_val, sem.id ?: "-")
                                        DataRow(Icons.Default.DateRange, "${stringResource(R.string.personal_sem)} ${sem.number_name ?: ""}", displayName)
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // --- 3. LEGAL & CITIZENSHIP ---
                        item {
                            SectionHeader(stringResource(R.string.personal_legal_citizenship), Icons.Outlined.AccountBalance)
                            InfoCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                DataRow(Icons.Outlined.Flag, stringResource(R.string.personal_citizenship), citizenshipDisplay)
                                DataRow(Icons.Outlined.Flag, stringResource(R.string.personal_nationality), nationalityDisplay)
                                DataRow(Icons.Default.Fingerprint, stringResource(R.string.personal_pin), pds?.pin)
                                DataRow(Icons.Default.Description, stringResource(R.string.personal_has_documents), pds?.is_have_document?.toString())
                                
                                if (isValid(pds?.passport_number) || isValid(pds?.release_organ)) {
                                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    DataRow(Icons.Default.Book, stringResource(R.string.personal_passport), pds?.getFullPassport())
                                    DataRow(Icons.Default.Business, stringResource(R.string.personal_authority), pds?.release_organ)
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_issued), pds?.release_date)
                                    DataRow(Icons.Default.Info, stringResource(R.string.personal_pds_info), pds?.info)
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        // --- 4. FAMILY INFORMATION ---
                        if (hasFamily) {
                            item {
                                SectionHeader(stringResource(R.string.personal_family_info), Icons.Outlined.FamilyRestroom)
                                InfoCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                    if (isValid(pds?.father_full_name)) {
                                        Text(stringResource(R.string.personal_father), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                        DataRow(Icons.Default.Person, stringResource(R.string.personal_name), pds?.father_full_name)
                                        DataRow(Icons.Default.Phone, stringResource(R.string.personal_phone), pds?.father_phone)
                                        DataRow(Icons.Default.Info, stringResource(R.string.personal_info), pds?.father_info)
                                    }
                                    
                                    if (isValid(pds?.father_full_name) && isValid(pds?.mother_full_name)) {
                                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    
                                    if (isValid(pds?.mother_full_name)) {
                                        Text(stringResource(R.string.personal_mother), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                        DataRow(Icons.Default.Person, stringResource(R.string.personal_name), pds?.mother_full_name)
                                        DataRow(Icons.Default.Phone, stringResource(R.string.personal_phone), pds?.mother_phone)
                                        DataRow(Icons.Default.Info, stringResource(R.string.personal_info), pds?.mother_info)
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // --- 5. GEOGRAPHY ---
                        if (hasGeography) {
                            item {
                                SectionHeader(stringResource(R.string.personal_geography), Icons.Outlined.Place)
                                InfoCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                    if (isValid(pds?.address)) {
                                        Text(stringResource(R.string.personal_main_address), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        DataRow(Icons.Default.Home, stringResource(R.string.personal_address), pds?.address)
                                        
                                        val country = IdDefinitions.getCountryName(pds?.id_country, currentLang)
                                        val oblast = IdDefinitions.getOblastName(pds?.id_oblast, currentLang)
                                        val region = IdDefinitions.getRegionName(pds?.id_region, currentLang)
                                        
                                        if (isValid(country)) DataRow(Icons.Default.Public, stringResource(R.string.personal_country), stringResource(R.string.fmt_name_id, country, pds?.id_country ?: "-"))
                                        if (isValid(oblast)) DataRow(Icons.Default.Public, stringResource(R.string.personal_state), stringResource(R.string.fmt_name_id, oblast, pds?.id_oblast ?: "-"))
                                        if (isValid(region)) DataRow(Icons.Default.Public, stringResource(R.string.personal_region), stringResource(R.string.fmt_name_id, region, pds?.id_region ?: "-"))
                                    }

                                    if (isValid(pds?.birth_address)) {
                                        if (isValid(pds?.address)) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                        
                                        Text(stringResource(R.string.personal_birth_place), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        DataRow(Icons.Default.Home, stringResource(R.string.personal_birth_addr), pds?.birth_address)
                                        val bCountry = IdDefinitions.getCountryName(pds?.id_birth_country, currentLang)
                                        val bOblast = IdDefinitions.getOblastName(pds?.id_birth_oblast, currentLang)
                                        val bRegion = IdDefinitions.getRegionName(pds?.id_birth_region, currentLang)
                                        if (isValid(bCountry)) DataRow(Icons.Default.Public, stringResource(R.string.personal_b_country), stringResource(R.string.fmt_name_id, bCountry, pds?.id_birth_country ?: "-"))
                                        if (isValid(bOblast)) DataRow(Icons.Default.Public, stringResource(R.string.personal_b_state), stringResource(R.string.fmt_name_id, bOblast, pds?.id_birth_oblast ?: "-"))
                                        if (isValid(bRegion)) DataRow(Icons.Default.Public, stringResource(R.string.personal_b_region), stringResource(R.string.fmt_name_id, bRegion, pds?.id_birth_region ?: "-"))
                                    }

                                    if (isValid(pds?.residence_address)) {
                                        if (isValid(pds?.birth_address) || isValid(pds?.address)) 
                                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                        Text(stringResource(R.string.personal_residence_place), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        DataRow(Icons.Default.Home, stringResource(R.string.personal_res_addr), pds?.residence_address)
                                        val rCountry = IdDefinitions.getCountryName(pds?.id_residence_country, currentLang)
                                        val rOblast = IdDefinitions.getOblastName(pds?.id_residence_oblast, currentLang)
                                        val rRegion = IdDefinitions.getRegionName(pds?.id_residence_region, currentLang)
                                        if (isValid(rCountry)) DataRow(Icons.Default.Public, stringResource(R.string.personal_r_country), stringResource(R.string.fmt_name_id, rCountry, pds?.id_residence_country ?: "-"))
                                        if (isValid(rOblast)) DataRow(Icons.Default.Public, stringResource(R.string.personal_r_state), stringResource(R.string.fmt_name_id, rOblast, pds?.id_residence_oblast ?: "-"))
                                        if (isValid(rRegion)) DataRow(Icons.Default.Public, stringResource(R.string.personal_r_region), stringResource(R.string.fmt_name_id, rRegion, pds?.id_residence_region ?: "-"))
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // --- 6. MILITARY SERVICE ---
                        if (hasMilitary) {
                            item {
                                SectionHeader(stringResource(R.string.personal_military_service), Icons.Default.Shield)
                                ExpandableCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                    DataRow(Icons.Default.Shield, stringResource(R.string.personal_service), military?.name ?: military?.name_military)
                                    DataRow(Icons.Default.Badge, stringResource(R.string.personal_name), military?.name)
                                    DataRow(Icons.Default.Badge, stringResource(R.string.personal_serial), military?.serial_number)
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_date), military?.date)
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_record_created), military?.created_at)
                                    DataRow(Icons.Default.Settings, stringResource(R.string.personal_id), military?.id?.takeIf { it != 0 }?.toString())
                                    DataRow(Icons.Default.Settings, stringResource(R.string.personal_student_id), military?.id_student?.takeIf { it != 0L }?.toString())
                                    DataRow(Icons.Default.Settings, stringResource(R.string.personal_user_id), military?.id_user?.takeIf { it != 0L }?.toString())
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_updated), military?.updated_at)
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // --- 7. MOVEMENT & FINANCE ---
                        if (hasMovement) {
                            item {
                                SectionHeader(stringResource(R.string.personal_movement_finance), Icons.Outlined.History)
                                ExpandableCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                    Text(stringResource(R.string.personal_movement), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    DataRow(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.personal_type), movTypeObj?.getName(currentLang))
                                    DataRow(Icons.Default.CheckCircle, stringResource(R.string.personal_is_student), movTypeObj?.is_student?.toString())
                                    DataRow(Icons.Default.Description, stringResource(R.string.personal_desc), mov?.info_description)
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_period), periodName)
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_start_date), periodObj?.start)
                                    DataRow(Icons.Default.CheckCircle, stringResource(R.string.personal_period_active), periodObj?.active?.toString())
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_order), mov?.info)
                                    DataRow(Icons.Default.Settings, stringResource(R.string.personal_itngyrg), mov?.itngyrg?.toString()?.takeUnless { it == "0" })
                                    DataRow(Icons.Default.Translate, stringResource(R.string.personal_state_lang_lvl), mov?.id_state_language_level?.takeIf { it != 0 }?.toString())
                                    
                                    DataRow(Icons.Default.DateRange, stringResource(R.string.personal_mov_edu_year), mov?.id_edu_year?.takeIf { it != 0 }?.toString())
                                    DataRow(Icons.Default.Settings, stringResource(R.string.personal_archive_user_id), mov?.id_import_archive_user?.takeIf { it != 0 }?.toString())
                                    DataRow(Icons.Default.Settings, stringResource(R.string.personal_mov_citizenship), mov?.citizenship?.takeIf { it != 0 }?.toString())
                                    DataRow(Icons.Default.Settings, stringResource(R.string.personal_mov_oo1_id), mov?.id_oo1?.takeIf { it != 0 }?.toString())
                                    DataRow(null, stringResource(R.string.personal_mov_zo1_id), mov?.id_zo1?.takeIf { it != 0 }?.toString())
                                    
                                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    
                                    Text(stringResource(R.string.personal_library_finance), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    val libStatus = if (profile?.is_library_debt == true) stringResource(R.string.lib_has_debt) else stringResource(R.string.lib_clean)
                                    DataRow(Icons.Default.Book, stringResource(R.string.personal_library), libStatus)
                                    DataRow(Icons.Default.Money, stringResource(R.string.personal_debt_credits), profile?.access_debt_credit_count?.toString())
                                    
                                    DataRow(Icons.AutoMirrored.Filled.List, stringResource(R.string.personal_lib_items), "${profile?.studentlibrary?.size ?: 0}")
                                    DataRow(Icons.AutoMirrored.Filled.List, stringResource(R.string.personal_debt_trans), "${profile?.student_debt_transcript?.size ?: 0}")
                                    DataRow(Icons.AutoMirrored.Filled.List, stringResource(R.string.personal_total_price), "${profile?.total_price?.size ?: 0}")
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // --- 8. SYSTEM METADATA ---
                        if (hasSystem) {
                            item {
                                SectionHeader(stringResource(R.string.personal_system_metadata), Icons.Outlined.Terminal)
                                ExpandableCard(glassmorphismEnabled = vm.glassmorphismEnabled) {
                                    Text(stringResource(R.string.personal_user_profile), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    MetaDataRow(stringResource(R.string.personal_user_id), user?.id_user?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_uni_id_user), user?.id_university?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_uni_id_mov), mov?.id_university?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_pds_id), pds?.id?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_pds_user_id), pds?.id_user?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_movement_id), (mov?.id_movement ?: mov?.id)?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_legacy_avn_id), user?.id_avn?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_legacy_aryz_id), user?.id_aryz?.takeIf { it != 0L })
                                    
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    
                                    Text(stringResource(R.string.personal_timestamps), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    MetaDataRow(stringResource(R.string.personal_user_created), user?.created_at)
                                    MetaDataRow(stringResource(R.string.personal_user_updated), user?.updated_at)
                                    MetaDataRow(stringResource(R.string.personal_pds_created), pds?.created_at)
                                    MetaDataRow(stringResource(R.string.personal_pds_updated), pds?.updated_at)
                                    MetaDataRow(stringResource(R.string.personal_mov_created), mov?.created_at)
                                    MetaDataRow(stringResource(R.string.personal_mov_updated), mov?.updated_at)
                                    
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    
                                    Text(stringResource(R.string.personal_structure_ids), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    MetaDataRow(stringResource(R.string.personal_fac_type_id), facObj?.id_faculty_type?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_direction_id), specObj?.id_direction?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_group_edu_form), eduObj?.id?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_oo1_id), specObj?.id_oo1?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_zo1_id), specObj?.id_zo1?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_exam_type_id), pds?.id_exam_type?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_round_id), pds?.id_round?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_tariff_type_id), mov?.id_tariff_type?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_avn_group_id), mov?.id_avn_group?.takeIf { it != 0 })
                                    
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    
                                    Text(stringResource(R.string.personal_registry_data), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    MetaDataRow(stringResource(R.string.personal_faculty_id), facObj?.id?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_fac_user_id), facObj?.id_user?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_fac_uni_id), facObj?.id_university?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_fac_created), facObj?.created_at)
                                    MetaDataRow(stringResource(R.string.personal_spec_id), specObj?.id?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_spec_user_id), specObj?.id_user?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_spec_uni_id), specObj?.id_university?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_spec_created), specObj?.created_at)
                                    MetaDataRow(stringResource(R.string.personal_edu_id), eduObj?.id?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_edu_user_id), eduObj?.id_user?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_edu_uni_id), eduObj?.id_university?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_edu_created), eduObj?.created_at)
                                    MetaDataRow(stringResource(R.string.personal_pay_id), payObj?.id?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_pay_user_id), payObj?.id_user?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_pay_uni_id), payObj?.id_university?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_pay_created), payObj?.created_at)
                                    MetaDataRow(stringResource(R.string.personal_lang_id), langObj?.id?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_lang_user_id), langObj?.id_user?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_lang_uni_id), langObj?.id_university?.takeIf { it != 0L })
                                    MetaDataRow(stringResource(R.string.personal_citizenship_id), pds?.id_citizenship?.takeIf { it != 0 })
                                    MetaDataRow(stringResource(R.string.personal_nationality_id), pds?.id_nationality?.takeIf { it != 0 })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

