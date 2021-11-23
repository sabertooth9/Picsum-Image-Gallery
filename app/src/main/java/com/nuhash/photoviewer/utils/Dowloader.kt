package com.nuhash.photoviewer.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.nuhash.photoviewer.BuildConfig
import com.nuhash.photoviewer.model.ImageModel
import com.nuhash.photoviewer.utils.CommonFunction.logger
import java.io.File

object Downloader {
    fun stateDownload(activity: Activity, imageModel: ImageModel, shareImage: Boolean) {
        val manager: DownloadManager =
            activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val savedImagePath =
            activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
        val imageFileName =
            "JPEG_" + imageModel.title + "_" + imageModel.id + "_" + imageModel.width + "x" + imageModel.height + ".jpg"

        val request = DownloadManager.Request(Uri.parse(imageModel.link))

        request.setDestinationInExternalFilesDir(
            activity,
            Environment.DIRECTORY_PICTURES,
            imageFileName
        )
        val outputFile = File(savedImagePath, imageFileName)
        if (outputFile.exists()) outputFile.delete()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        val downloadManagerId = manager.enqueue(request)
        if (shareImage) {
            afterDownload(downloadManagerId, manager, activity, imageModel)
        }
    }

    private fun afterDownload(
        enqueue: Long,
        dm: DownloadManager,
        activity: Activity,
        imageModel: ImageModel
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
                    val query = DownloadManager.Query()
                    query.setFilterById(enqueue)
                    val cursor = dm.query(query)
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                            logger("DOWNLOAD", "" + dm.getUriForDownloadedFile(downloadId))
                            shareImage(activity, imageModel)
                        }
                    }
                    cursor.close()

                }
            }

        }
        activity.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun shareImage(activity: Activity, imageModel: ImageModel) {
        val savedImagePath =
            activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
        val imageFileName =
            "JPEG_" + imageModel.title + "_" + imageModel.id + "_" + imageModel.width + "x" + imageModel.height + ".jpg"
        val imageFile = File(savedImagePath, imageFileName)
        logger("savedImagePath", savedImagePath)
        logger("fileName", imageFileName)

        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/*"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val photoUri = FileProvider.getUriForFile(
                activity,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                imageFile
            )
            logger("photoUri", photoUri.toString())
            //share.data = photoUri
            share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            share.putExtra(
                Intent.EXTRA_STREAM,
                photoUri
            )

        } else {
            share.putExtra(
                Intent.EXTRA_STREAM,
                Uri.fromFile(imageFile)
            )
        }
        activity.startActivity(Intent.createChooser(share, "Share via"))
    }
}