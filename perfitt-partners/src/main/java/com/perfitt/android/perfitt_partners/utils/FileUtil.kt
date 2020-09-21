package com.perfitt.android.perfitt_partners.utils

import android.content.Context
import java.io.File

class FileUtil {

    /**
     * 리뷰 이미지 캐시 디렉토리 패스 가져오기
     *
     * @param context context
     * @return
     */
    fun getReviewFilePath(context: Context): File {
        val file = File(context.cacheDir.path + REVIEW_IMAGES)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * 저장된 리뷰 이미지 파일 삭제 하기
     */
    fun deleteReviewImageFile(context: Context) {
        getReviewFilePath(context).run {
            if (exists()) {
                val childFileList = listFiles() ?: return
                for (childFile in childFileList) {
                    if (childFile.isDirectory) {
                        //하위 디렉토리 루프
                        childFile.absolutePath
                    } else {
                        //하위 파일삭제
                        childFile.delete()
                    }
                }
            }
        }
    }

    /**
     * 발 이미지 캐시 디렉토리 패스 가져오기
     *
     * @param context context
     * @return
     */
    fun getFootFilePath(context: Context): File {
        val file = File(context.cacheDir.path + FOOT_IMAGES)
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * 저장된 리뷰 이미지 파일 삭제 하기
     */
    fun deleteFootImageFile(context: Context) {
        getFootFilePath(context).run {
            if (exists()) {
                val childFileList = listFiles() ?: return
                for (childFile in childFileList) {
                    if (childFile.isDirectory) {
                        //하위 디렉토리 루프
                        childFile.absolutePath
                    } else {
                        //하위 파일삭제
                        childFile.delete()
                    }
                }
            }
        }
    }

    private object Holder {
        val INSTANCE = FileUtil()
    }

    companion object {
        val instance: FileUtil by lazy { Holder.INSTANCE }
        const val REVIEW_IMAGES = "/review"
        const val FOOT_IMAGES = "/foot"
        const val FILE_NAME_FOOT_RIGHT = "foot_right.jpg"
        const val FILE_NAME_FOOT_LEFT = "foot_left.jpg"
    }
}