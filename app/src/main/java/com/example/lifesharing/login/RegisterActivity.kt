package com.example.lifesharing.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Path
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import com.example.lifesharing.R
import com.example.lifesharing.databinding.ActivityRegisterBinding
import com.example.lifesharing.login.model.request_body.LocationDTO
import com.example.lifesharing.login.model.request_body.RegisterRequestBody
import com.example.lifesharing.login.viewModel.RegisterViewModel
import com.example.lifesharing.service.work.RegisterWork
import com.example.lifesharing.util.RequestPermissionsUtil
import com.google.android.gms.location.LocationServices
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    val TAG: String = "로그"
    lateinit var binding: ActivityRegisterBinding
    val registerViewModel: RegisterViewModel by viewModels()

    var imagePath: String? = null

    var email: MutableLiveData<String> = MutableLiveData("")
    var password: MutableLiveData<String> = MutableLiveData("")
    var name: MutableLiveData<String> = MutableLiveData("")
    var checkPassword: MutableLiveData<String> = MutableLiveData("")
    var phone: MutableLiveData<String> = MutableLiveData("")
    var verifiedNumber: MutableLiveData<String> = MutableLiveData("")

    private lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    lateinit var mediaPath: Path

    lateinit var body: MultipartBody.Part

    lateinit var roadAdd: String

    lateinit var emdNm : String

//    override fun onStart() {
//        super.onStart()
//    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        binding.viewModel = registerViewModel
        binding.activity = this
        binding.lifecycleOwner = this

        body = MultipartBody.Part.createFormData("multipartFile", "","".toRequestBody("image/*".toMediaTypeOrNull()))

        pickMedia = registerForActivityResult(/* contract = */
            ActivityResultContracts.PickVisualMedia()
        ) /* callback = */

        { uri ->
            if (uri != null) {
                Log.d(TAG, "pickImage: $uri")
                imagePath = uri.toString()
                Log.d(TAG, "pickImage: $imagePath 가 잘 들어왔나요 ??")
                //var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imagePath!!.toUri())
                var bitmap: Bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                        contentResolver,
                        imagePath!!.toUri()
                    )
                )
                binding.myUserProfile.setImageBitmap(bitmap)

                var imagePath: String = getImagePath(uri)

                Log.d(TAG, "imagePath가 잘찍할까요 ?? 이거를 서버에 보내주면 되는데~ $imagePath ")

                val file = File(imagePath)
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                body = MultipartBody.Part.createFormData("multipartFile", file.name, requestFile)

            } else {
                Log.d(TAG, "pickImage: no media selected")
            }
        }

        setObserve()
    }

    fun registerWithImage() {
        val registerUserData = RegisterRequestBody(
            email.value.toString(),
            password.value.toString(),
            name.value.toString(),
            phone.value.toString(),
            LocationDTO(roadAdd, emdNm)
        )


        val retrofitWork = RegisterWork(registerUserData, body)
        retrofitWork.registerWork()
    }


    fun getImagePath(uri: Uri?): String {
        val proj: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(uri!!, proj, null, null, null)
        val index = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()

        var result = cursor?.getString(index!!)

        return result!!
    }

    fun pickImage() {
        try {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            Log.d(TAG, "${e.message}")
        }

    }

    fun nicknameCheck() {
        binding.registerNicknameCheck.setBackgroundResource(R.drawable.register_location_btn)
        binding.registerNicknameCheck.setTextColor(Color.parseColor("#1277ED"))
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    fun getLocation() {
        binding.registerLocationBtn.setBackgroundResource(R.drawable.register_location_btn)
        binding.registerLocationBtn.text = "위치 인증 완료"
        binding.registerLocationBtn.setTextColor(Color.parseColor("#1277ED"))

        RequestPermissionsUtil(this).requestLocation()

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { success: Location? ->
                success?.let { location ->
                    Log.d(
                        TAG,
                        "현재 위경도가 잘 받아와질까요 ? 위도 : ${location.latitude}, 경도 : ${location.longitude} "
                    )
                    val address = getAddress(location.latitude, location.longitude)?.get(0)
                    // adminArea 도 or 광역시
                    // locality 지역이름 시 혹은 군구 ?
                    // thoroughfore 동

                    roadAdd = address?.getAddressLine(0)!!
                    emdNm = address.thoroughfare!!

                    Log.d(TAG, roadAdd)
                    Log.d(TAG, "실제 위도: $emdNm") // ${address.getAddressLine(0)}")
                }
            }

        revokeSelfPermissionOnKill("")
    }

    private fun getAddress(lat: Double, lng: Double): List<Address>? {
        lateinit var address: List<Address>

        return try {
            val geocoder = Geocoder(this, Locale.KOREA)
            address = geocoder.getFromLocation(lat, lng, 1) as List<Address>
            address
        } catch (e: IOException) {
            Toast.makeText(this, "주소를 가져 올 수 없습니다.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun checkAll() {
        binding.registerAllCheck.setImageResource(R.drawable.register_all_check)
        binding.registerCheck1.setImageResource(R.drawable.register_check_color)
        binding.registerCheck2.setImageResource(R.drawable.register_check_color)
        binding.registerButton.setBackgroundResource(R.drawable.register_colored_btn)
        binding.registerButton.setTextColor(Color.parseColor("#ffffff"))
    }

    fun check1() {
        binding.registerCheck1.setImageResource(R.drawable.register_check_color)
    }

    fun check2() {
        binding.registerCheck2.setImageResource(R.drawable.register_check_color)
    }


    fun setObserve() {
        registerViewModel.showLoginActivity.observe(this) {
            if (it) {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }
}
