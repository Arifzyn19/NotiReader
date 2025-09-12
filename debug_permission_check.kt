// Add this method to MainActivity to check MediaStore permissions
private fun checkMediaStorePermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissions = arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.d("xyz", "Missing MediaStore permissions: $missingPermissions")
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        } else {
            Log.d("xyz", "All MediaStore permissions granted")
        }
    } else {
        // For older versions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("xyz", "Missing READ_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }
    }
}

