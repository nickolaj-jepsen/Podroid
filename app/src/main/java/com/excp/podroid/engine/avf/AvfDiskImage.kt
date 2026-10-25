/*
 * Podroid - Rootless Podman for Android
 * Copyright (C) 2024-2026 Podroid contributors
 *
 * AvfDiskImage — an imported raw GPT disk image (e.g. a NixOS image) that AVF
 * boots through its bundled u-boot instead of the Alpine kernel + squashfs.
 * While the file exists AvfEngine boots it; removing it returns to Alpine.
 */
package com.excp.podroid.engine.avf

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object AvfDiskImage {
    private const val FILE_NAME = "disk.img"

    /** AVF's EFI loader; it finds the image's ESP and runs the image's own boot loader. */
    const val UBOOT_PATH = "/apex/com.android.virt/etc/u-boot.bin"

    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun isPresent(context: Context): Boolean = file(context).exists()

    /**
     * Copies the image at [uri] into the app's files dir, replacing any previous
     * one only once the copy is complete. [onProgress] gets bytes copied so far.
     *
     * @throws IOException if the source can't be opened or the copy fails.
     */
    suspend fun import(context: Context, uri: Uri, onProgress: (Long) -> Unit) = withContext(Dispatchers.IO) {
        val target = file(context)
        val tmp = File(context.filesDir, "$FILE_NAME.part")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Couldn't open $uri")
            input.use { src ->
                tmp.outputStream().use { dst ->
                    val buf = ByteArray(1 shl 20)
                    var total = 0L
                    while (true) {
                        ensureActive()
                        val n = src.read(buf)
                        if (n < 0) break
                        dst.write(buf, 0, n)
                        total += n
                        onProgress(total)
                    }
                    dst.fd.sync()
                }
            }
            if (!tmp.renameTo(target)) throw IOException("Couldn't move the image into place")
        } finally {
            tmp.delete()
        }
    }

    fun remove(context: Context): Boolean = file(context).delete()
}
