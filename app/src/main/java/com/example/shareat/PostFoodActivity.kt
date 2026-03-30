package com.example.shareat

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Calendar
import java.util.Locale

class PostFoodActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private var selectedImageUri: Uri? = null

    private lateinit var radioDonate: RadioButton
    private lateinit var radioSell: RadioButton

    private lateinit var mealName: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var ingredients: EditText
    private lateinit var foodDescription: EditText

    private lateinit var allergenDairy: CheckBox
    private lateinit var allergenGluten: CheckBox
    private lateinit var allergenEggs: CheckBox
    private lateinit var allergenShellfish: CheckBox
    private lateinit var allergenNuts: CheckBox
    private lateinit var allergenSoy: CheckBox
    private lateinit var allergenFish: CheckBox
    private lateinit var allergenSesame: CheckBox

    private lateinit var veg: CheckBox
    private lateinit var vegan: CheckBox
    private lateinit var glutenFree: CheckBox
    private lateinit var halal: CheckBox
    private lateinit var kosher: CheckBox

    private lateinit var quantity: EditText
    private lateinit var unitSpinner: Spinner

    private lateinit var location: EditText
    private lateinit var pickupDate: EditText
    private lateinit var startTime: EditText
    private lateinit var endTime: EditText

    private lateinit var postDonation: Button
    private lateinit var foodImage: ImageView
    private lateinit var locationPicker: ImageButton
    private lateinit var speechButton: ImageButton

    private val LOCATION_PERMISSION_CODE = 100

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                imageBitmap?.let {
                    foodImage.setImageBitmap(it)
                    selectedImageUri = null
                    Toast.makeText(
                        this,
                        "Camera preview set. Gallery image is recommended for upload.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                foodImage.setImageURI(it)
            }
        }

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val spokenText = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.get(0)

                spokenText?.let {
                    foodDescription.setText(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_food)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        initializeViews()
        setupSpinners()
        setupImagePicker()
        setupLocationPicker()
        setupSpeechInput()
        setupDatePicker()
        setupTimePickers()
        setupPostButton()
    }

    private fun initializeViews() {
        radioDonate = findViewById(R.id.radioDonate)
        radioSell = findViewById(R.id.radioSell)

        mealName = findViewById(R.id.mealName)
        categorySpinner = findViewById(R.id.categorySpinner)
        ingredients = findViewById(R.id.ingredients)
        foodDescription = findViewById(R.id.foodDescription)
        speechButton = findViewById(R.id.speechButton)

        allergenDairy = findViewById(R.id.allergenDairy)
        allergenGluten = findViewById(R.id.allergenGluten)
        allergenEggs = findViewById(R.id.allergenEggs)
        allergenShellfish = findViewById(R.id.allergenShellfish)
        allergenNuts = findViewById(R.id.allergenNuts)
        allergenSoy = findViewById(R.id.allergenSoy)
        allergenFish = findViewById(R.id.allergenFish)
        allergenSesame = findViewById(R.id.allergenSesame)

        veg = findViewById(R.id.veg)
        vegan = findViewById(R.id.vegan)
        glutenFree = findViewById(R.id.glutenFree)
        halal = findViewById(R.id.halal)
        kosher = findViewById(R.id.kosher)

        quantity = findViewById(R.id.quantity)
        unitSpinner = findViewById(R.id.unitSpinner)

        location = findViewById(R.id.location)
        locationPicker = findViewById(R.id.locationPicker)

        pickupDate = findViewById(R.id.pickupDate)
        startTime = findViewById(R.id.startTime)
        endTime = findViewById(R.id.endTime)

        postDonation = findViewById(R.id.postDonation)
        foodImage = findViewById(R.id.foodImage)
    }

    private fun setupSpinners() {
        val categories = arrayOf(
            "Homemade",
            "Baked Goods",
            "Fruits",
            "Vegetables",
            "Beverages",
            "Packaged Foods"
        )

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        categorySpinner.adapter = categoryAdapter

        val units = arrayOf(
            "servings",
            "grams",
            "pieces",
            "portions"
        )

        val unitAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            units
        )
        unitSpinner.adapter = unitAdapter
    }

    private fun setupPostButton() {
        postDonation.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (selectedImageUri == null) {
                Toast.makeText(this, "Please upload a food image from gallery", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val ownerName = currentUser.displayName ?: "User"

            val meal = mealName.text.toString().trim()
            val selectedCategory = categorySpinner.selectedItem.toString()
            val description = foodDescription.text.toString().trim()
            val ingredientsText = ingredients.text.toString().trim()
            val qtyText = quantity.text.toString().trim()
            val unit = unitSpinner.selectedItem.toString()
            val pickupLocation = location.text.toString().trim()
            val pickupDateText = pickupDate.text.toString().trim()
            val startTimeText = startTime.text.toString().trim()
            val endTimeText = endTime.text.toString().trim()

            if (meal.isEmpty()) {
                mealName.error = "Meal name required"
                mealName.requestFocus()
                return@setOnClickListener
            }

            if (description.isEmpty()) {
                foodDescription.error = "Description required"
                foodDescription.requestFocus()
                return@setOnClickListener
            }

            if (qtyText.isEmpty()) {
                quantity.error = "Quantity required"
                quantity.requestFocus()
                return@setOnClickListener
            }

            val qty = qtyText.toIntOrNull()
            if (qty == null || qty <= 0) {
                quantity.error = "Enter a valid quantity"
                quantity.requestFocus()
                return@setOnClickListener
            }

            if (pickupLocation.isEmpty()) {
                location.error = "Pickup location required"
                location.requestFocus()
                return@setOnClickListener
            }

            if (pickupDateText.isEmpty()) {
                pickupDate.error = "Pickup date required"
                pickupDate.requestFocus()
                return@setOnClickListener
            }

            if (startTimeText.isEmpty()) {
                startTime.error = "Start time required"
                startTime.requestFocus()
                return@setOnClickListener
            }

            if (endTimeText.isEmpty()) {
                endTime.error = "End time required"
                endTime.requestFocus()
                return@setOnClickListener
            }

            val postType = if (radioSell.isChecked) "sell" else "donate"

            val allergenInformation = mutableListOf<String>()
            if (allergenDairy.isChecked) allergenInformation.add("Dairy")
            if (allergenGluten.isChecked) allergenInformation.add("Gluten")
            if (allergenEggs.isChecked) allergenInformation.add("Eggs")
            if (allergenShellfish.isChecked) allergenInformation.add("Shellfish")
            if (allergenNuts.isChecked) allergenInformation.add("Nuts")
            if (allergenSoy.isChecked) allergenInformation.add("Soy")
            if (allergenFish.isChecked) allergenInformation.add("Fish")
            if (allergenSesame.isChecked) allergenInformation.add("Sesame")

            val dietaryLabels = mutableListOf<String>()
            if (veg.isChecked) dietaryLabels.add("Vegetarian")
            if (vegan.isChecked) dietaryLabels.add("Vegan")
            if (glutenFree.isChecked) dietaryLabels.add("Gluten-Free")
            if (halal.isChecked) dietaryLabels.add("Halal")
            if (kosher.isChecked) dietaryLabels.add("Kosher")

            val newDocRef = db.collection("foods").document()
            val currentTime = System.currentTimeMillis()

            val foodMap = hashMapOf<String, Any>(
                "post_id" to newDocRef.id,
                "owner_id" to currentUser.uid,
                "owner_name" to ownerName,
                "title" to meal,
                "description" to description,
                "ingredients" to ingredientsText,
                "category" to selectedCategory,
                "quantity" to qty,
                "unit" to unit,
                "dietary_labels" to dietaryLabels,
                "allergen_information" to allergenInformation,
                "pickup_location" to pickupLocation,
                "pickup_date" to pickupDateText,
                "start_time" to startTimeText,
                "end_time" to endTimeText,
                "post_type" to postType,
                "status" to "available",
                "image_url" to "",
                "created_at" to currentTime,
                "updated_at" to currentTime
            )

            uploadImageAndSavePost(newDocRef.id, foodMap)
        }
    }

    private fun uploadImageAndSavePost(
        postId: String,
        foodMap: HashMap<String, Any>
    ) {
        val imageUri = selectedImageUri ?: return
        val imageRef = storageRef.child("food_images/$postId.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        foodMap["image_url"] = downloadUri.toString()
                        saveFoodPost(postId, foodMap)
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
                    "Image upload failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun saveFoodPost(postId: String, foodMap: HashMap<String, Any>) {
        db.collection("foods")
            .document(postId)
            .set(foodMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Food posted successfully", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupImagePicker() {
        foodImage.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Upload Food Photo")

            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        cameraLauncher.launch(intent)
                    }
                    1 -> {
                        galleryLauncher.launch("image/*")
                    }
                }
            }

            builder.show()
        }
    }

    private fun setupLocationPicker() {
        locationPicker.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_CODE
                )
            } else {
                fetchLocation()
            }
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { userLocation ->
            if (userLocation != null) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(
                    userLocation.latitude,
                    userLocation.longitude,
                    1
                )

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0].getAddressLine(0)
                    location.setText(address)
                } else {
                    Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                fetchLocation()
            }
        }
    }

    private fun setupSpeechInput() {
        speechButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Describe the food"
            )

            speechLauncher.launch(intent)
        }
    }

    private fun setupDatePicker() {
        pickupDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedMonth = String.format("%02d", selectedMonth + 1)
                    val formattedDay = String.format("%02d", selectedDay)
                    val formattedDate = "$selectedYear-$formattedMonth-$formattedDay"
                    pickupDate.setText(formattedDate)
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }
    }

    private fun setupTimePickers() {
        startTime.setOnClickListener {
            showTimePicker(startTime)
        }

        endTime.setOnClickListener {
            showTimePicker(endTime)
        }
    }

    private fun showTimePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                targetEditText.setText(formattedTime)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }
}