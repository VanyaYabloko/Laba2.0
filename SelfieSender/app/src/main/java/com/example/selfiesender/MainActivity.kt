package com.example.selfiesender

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var selfieImageView: ImageView
    private lateinit var takeSelfieButton: Button
    private lateinit var sendSelfieButton: Button
    private var currentPhotoPath: String? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        selfieImageView = findViewById(R.id.selfieImageView)
        takeSelfieButton = findViewById(R.id.takeSelfieButton)
        sendSelfieButton = findViewById(R.id.sendSelfieButton)
        sendSelfieButton.isEnabled = false

        takeSelfieButton.setOnClickListener {
            checkCameraPermission()
        }

        sendSelfieButton.setOnClickListener {
            sendEmailWithSelfie()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(
                    this,
                    "Для работы приложения требуется доступ к камере",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: Exception) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(this, "Доступ к камере запрещен", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            currentPhotoPath?.let { path ->
                val bitmap = BitmapFactory.decodeFile(path)
                selfieImageView.setImageBitmap(bitmap)
                sendSelfieButton.isEnabled = true
            }
        }
    }

    private fun sendEmailWithSelfie() {
        // Проверяем, есть ли сохраненное фото
        if (currentPhotoPath == null) {
            Toast.makeText(this, "Сначала сделайте селфи!", Toast.LENGTH_SHORT).show()
            return
        }

        // Создаем Intent для отправки email
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822" // MIME тип для email
            putExtra(Intent.EXTRA_EMAIL, arrayOf("hodovychenko@op.edu.ua")) // Адрес получателя
            putExtra(
                Intent.EXTRA_SUBJECT,
                "ANDROID Иванов Иван"
            ) // Тема письма (замените на свои данные)

            // Текст письма с ссылкой на репозиторий
            val emailText = "Привет!\n\nОтправляю свое селфи из приложения.\n\n" +
                    "Ссылка на репозиторий проекта:\n" +
                    "https://github.com/ваш_логин/название_репозитория"
            putExtra(Intent.EXTRA_TEXT, emailText)

            // Прикрепляем файл с селфи
            val photoFile = File(currentPhotoPath)
            val photoUri = FileProvider.getUriForFile(
                this@MainActivity,
                "${packageName}.provider",
                photoFile
            )
            putExtra(Intent.EXTRA_STREAM, photoUri)

            // Даем временные права на чтение файла
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Відправити email..."))
        } catch (ex: Exception) {
            Toast.makeText(this, "Не знайдено додатків для відправлення email", Toast.LENGTH_SHORT)
                .show()
        }
    }
}