package com.example.shareat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.material.icons.filled.Phone

class ProfileActivity : ComponentActivity() {

    private val selectedImageUri = mutableStateOf<Uri?>(null)
    private val uploadedImageUrl = mutableStateOf("")

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedImageUri.value = uri
            uri?.let { uploadProfileImage(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfileScreen(
                onEditPhotoClick = {
                    imagePickerLauncher.launch("image/*")
                },
                externalSelectedImageUri = selectedImageUri,
                externalUploadedImageUrl = uploadedImageUrl
            )
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val imageBytes = inputStream?.readBytes()
            inputStream?.close()

            if (imageBytes == null) {
                Toast.makeText(this, "Unable to read selected image", Toast.LENGTH_LONG).show()
                return
            }

            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("profile_images/${currentUser.uid}.jpg")

            storageRef.putBytes(imageBytes)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            uploadedImageUrl.value = downloadUri.toString()

                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.uid)
                                .update(
                                    mapOf(
                                        "profile_image_url" to downloadUri.toString(),
                                        "updated_at" to System.currentTimeMillis()
                                    )
                                )

                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setPhotoUri(downloadUri)
                                .build()

                            currentUser.updateProfile(profileUpdates)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Profile picture updated",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Photo updated in Firestore, but auth profile failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to get image URL: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Profile image upload failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error reading selected image: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
fun ProfileScreen(
    onEditPhotoClick: () -> Unit,
    externalSelectedImageUri: MutableState<Uri?>,
    externalUploadedImageUrl: MutableState<String>
) {
    val fullName = remember { mutableStateOf("User") }
    val email = remember { mutableStateOf("") }
    val phoneNumber = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("SharEat Community") }
    val bio = remember {
        mutableStateOf("Helping reduce food waste and sharing meals with the community.")
    }
    val profileImageUrl = remember { mutableStateOf("") }
    var showEditBioDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            email.value = currentUser.email ?: ""
            if (!currentUser.displayName.isNullOrEmpty()) {
                fullName.value = currentUser.displayName!!
            }
            if (currentUser.photoUrl != null) {
                profileImageUrl.value = currentUser.photoUrl.toString()
            }

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val firestoreName = document.getString("full_name")
                    val firestoreImage = document.getString("profile_image_url")
                    val firestoreBio = document.getString("bio")
                    val firestoreLocation = document.getString("location")
                    val firestorePhone = document.getString("phone_number")

                    if (!firestoreName.isNullOrEmpty()) fullName.value = firestoreName
                    if (!firestoreImage.isNullOrEmpty()) profileImageUrl.value = firestoreImage
                    if (!firestoreBio.isNullOrEmpty()) bio.value = firestoreBio
                    if (!firestoreLocation.isNullOrEmpty()) location.value = firestoreLocation
                    if (!firestorePhone.isNullOrEmpty()) phoneNumber.value = firestorePhone
                }
        }
    }

    LaunchedEffect(externalUploadedImageUrl.value) {
        if (externalUploadedImageUrl.value.isNotEmpty()) {
            profileImageUrl.value = externalUploadedImageUrl.value
        }
    }

    if (showEditBioDialog) {
        EditBioDialog(
            currentBio = bio.value,
            onDismiss = { showEditBioDialog = false },
            onSave = { newBio ->
                bio.value = newBio
                showEditBioDialog = false

                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .update(
                            mapOf(
                                "bio" to newBio,
                                "updated_at" to System.currentTimeMillis()
                            )
                        )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
            .verticalScroll(rememberScrollState())
    ) {
        HeaderSection(
            fullName = fullName.value,
            location = location.value,
            imageUrl = profileImageUrl.value,
            localImageUri = externalSelectedImageUri.value,
            onEditPhotoClick = onEditPhotoClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        AboutMeCard(
            bio = bio.value,
            onEditClick = { showEditBioDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AccountInfoCard(
            email = email.value,
            phoneNumber = phoneNumber.value
        )

        Spacer(modifier = Modifier.height(24.dp))

        QuickActions()

        Spacer(modifier = Modifier.height(24.dp))

        LogOutButton()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HeaderSection(
    fullName: String,
    location: String,
    imageUrl: String,
    localImageUri: Uri?,
    onEditPhotoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF23C483))
            .padding(top = 36.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                imageUrl = imageUrl,
                localImageUri = localImageUri,
                onEditPhotoClick = onEditPhotoClick
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = fullName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Don’t waste it, share it",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProfileAvatar(
    imageUrl: String,
    localImageUri: Uri?,
    onEditPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(82.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape),
            shape = CircleShape,
            color = Color.White
        ) {
            when {
                localImageUri != null -> {
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { imageView ->
                            Glide.with(imageView.context)
                                .load(localImageUri)
                                .circleCrop()
                                .into(imageView)
                        }
                    )
                }

                imageUrl.isNotEmpty() -> {
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { imageView ->
                            Glide.with(imageView.context)
                                .load(imageUrl)
                                .circleCrop()
                                .into(imageView)
                        }
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFF23C483),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(28.dp)
                .offset(x = 2.dp, y = 2.dp)
                .clickable { onEditPhotoClick() },
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit profile picture",
                    tint = Color(0xFF23C483),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AboutMeCard(
    bio: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "About Me",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E2B27),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Edit",
                    color = Color(0xFF23C483),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onEditClick() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = bio,
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun AccountInfoCard(
    email: String,
    phoneNumber: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Account Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E2B27)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color(0xFF23C483),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (email.isNotEmpty()) email else "No email available",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color(0xFF23C483),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (phoneNumber.isNotEmpty()) phoneNumber else "No phone number available",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun QuickActions() {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Quick Actions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E2B27)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionCard(
                icon = Icons.Default.Email,
                label = "Messages",
                modifier = Modifier.weight(1f)
            ) {
                val intent = Intent(context, MessagesActivity::class.java)
                context.startActivity(intent)
            }

            ActionCard(
                icon = Icons.Default.List,
                label = "My Meals",
                modifier = Modifier.weight(1f)
            ) {
                val intent = Intent(context, MyPostsActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun ActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF63706C),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LogOutButton() {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Log Out",
                color = Color(0xFFE53935),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EditBioDialog(
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var bioText by remember { mutableStateOf(currentBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Bio")
        },
        text = {
            OutlinedTextField(
                value = bioText,
                onValueChange = { bioText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bio") }
            )
        },
        confirmButton = {
            Button(onClick = { onSave(bioText) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}