package com.example.project.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.project.CheckInPrefs
import com.example.project.R
import com.example.project.activities.ScanActivity
import com.example.project.UserPrefs
import com.example.project.data.Check
import com.example.project.data.TimeManager

import com.example.project.databinding.FragmentHomeBinding
import com.example.project.function.function.showCustomToast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Home : Fragment() {

    private val requiredQrText = "Yakuza"

    private var checkInTime: Long? = null
    private var isCheckedIn = false

    private val timeFormatterHM = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    private val hoursFormat = SimpleDateFormat("HH", Locale.getDefault())
    private val minutesFormat = SimpleDateFormat("mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    private val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val now = System.currentTimeMillis()
    private val dayName = dayNameFormat.format(Date())
    private val day = dayFormat.format(Date(now))
    private val month = monthFormat.format(Date(now))
    private val year = yearFormat.format(Date(now))

    private val handler = Handler()
    private lateinit var timeRunnable: Runnable
    private lateinit var timeHourRunnable: Runnable

    private lateinit var binding: FragmentHomeBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    private lateinit var userId: String

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startQrScan()
            } else {
                requireContext().showCustomToast("Camera permission is required to scan", R.layout.error_toast)
            }
        }

    private val scanActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scannedText = result.data?.getStringExtra("SCAN_RESULT")
                scannedText?.let {
                    handleQrResult(it)
                }
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")
        val uid = auth.currentUser?.uid

        userId = UserPrefs.loadUserId(requireContext()).toString()

        binding.tvDate.text = "$month $day, $year - $dayName"


        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startQrScan()
        }

        val savedState = CheckInPrefs.loadCheckInState(requireContext(), userId)

        if (savedState.checkInStr != null) {
            binding.tvCheckInTime.text = savedState.checkInStr
            checkInTime = if (savedState.isCheckedIn) savedState.checkInMillis else null
            isCheckedIn = savedState.isCheckedIn
        }
        if (savedState.checkOutStr != null) {
            binding.tvCheckOutTime.text = savedState.checkOutStr
        }
        if (savedState.duration != null) {
            binding.tvTotalHours.text = savedState.duration
        }

        if (savedState.isCheckedIn) {
            binding.checkBtnName.text = "Check Out"
            binding.checkInBtn.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.checkOut
                )
            )
            binding.cardCheckInButton.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.checkOutLight
                )
            )
        } else {
            binding.checkBtnName.text = "Check In"
            binding.checkInBtn.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.mainColor
                )
            )
            binding.cardCheckInButton.setCardBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.secondColor
                )
            )
        }

        if (uid != null) {
            userRef.child(uid).child("lastName").get()
                .addOnSuccessListener {
                    val lastName = it.value.toString()
                    binding.tvGreeting.text = "Hey, $lastName"
                }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        startUpdatingTime()
    }

    private fun startQrScan() {
        binding.checkInBtn.setOnClickListener {
            val intent = Intent(requireContext(), ScanActivity::class.java)
            scanActivityLauncher.launch(intent)
        }
    }

    private fun handleQrResult(scannedText: String) {
        if (scannedText == requiredQrText) {
            val now = System.currentTimeMillis()
            if (isCheckedIn == false) {

                // First scan: Check-in
                checkInTime = now
                val checkInString = timeFormatterHM.format(Date(now))
                binding.tvCheckInTime.text = checkInString
                binding.checkBtnName.text = "Check Out"

                isCheckedIn = true
                CheckInPrefs.saveCheckIn(requireContext(), userId, true, now, checkInString)
                CheckInPrefs.saveCheckOut(requireContext(), userId, true, "00:00", "00:00")

                binding.tvCheckOutTime.text = "00:00"
                binding.tvTotalHours.text = "00:00"

                binding.checkInBtn.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.checkOut
                    )
                )
                binding.cardCheckInButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.checkOutLight
                    )
                )

            } else {
                // Second scan: Check-out
                checkOutFunction()
            }
        } else {
            requireContext().showCustomToast("Invalid QR Code", R.layout.error_toast)
        }
    }

    private fun checkOutFunction() {
        val now = System.currentTimeMillis()
        val checkOutString = timeFormatterHM.format(Date(now))
        binding.tvCheckOutTime.text = checkOutString
        binding.checkBtnName.text = "Check In"

        val durationMillis = now - (checkInTime ?: 0L)
        val durationInSeconds = durationMillis / 1000
        val hours = durationInSeconds / 3600
        val minutes = (durationInSeconds % 3600) / 60
        val durationStr = "${hours}h ${minutes}m"
        binding.tvTotalHours.text = durationStr

        CheckInPrefs.saveCheckOut(requireContext(), userId, false, checkOutString, durationStr)

        checkInTime = null
        isCheckedIn = false

        binding.checkInBtn.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.mainColor
            )
        )
        binding.cardCheckInButton.setCardBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.secondColor
            )
        )

        val savedState = CheckInPrefs.loadCheckInState(requireContext(), userId)
        var checkInTimeMillis = savedState.checkInStr
        checkInTimeMillis = checkInTimeMillis.toString()

        val currentDateStr = dateFormatter.format(Date(now))
        insertCheckToDatabase(currentDateStr, checkInTimeMillis, checkOutString, durationInSeconds)
    }

    private fun insertCheckToDatabase(
        date: String,
        checkInTime: String,
        checkOutTime: String,
        durationInSecond: Long
    ) {
        val check = Check(date, checkInTime, checkOutTime, durationInSecond)
        val uid = auth.currentUser?.uid
        if (uid != null) {
            userRef.child(uid).child("checks").push().setValue(check)
        }
        requireContext().showCustomToast("Successfully added check!", R.layout.success_toast)
    }

    private fun startUpdatingTime() {
        timeRunnable = object : Runnable {
            override fun run() {
                updateCurrentTimeText()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timeRunnable)
    }
    private fun updateCurrentTimeText() {
        val now = System.currentTimeMillis()
        val currentTimeStr = timeFormatter.format(Date(now))
        binding.tvTime.text = currentTimeStr
    }
}
