package com.demushrenich.archim.data.utils

import net.sf.sevenzipjbinding.IInStream
import net.sf.sevenzipjbinding.SevenZipException
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class FileDescriptorInStream(fd: FileDescriptor) : IInStream {
    private val channel: FileChannel = FileInputStream(fd).channel

    @Throws(SevenZipException::class)
    override fun seek(offset: Long, seekOrigin: Int): Long {
        return try {
            when (seekOrigin) {
                IInStream.SEEK_SET -> {
                    channel.position(offset)
                }
                IInStream.SEEK_CUR -> {
                    channel.position(channel.position() + offset)
                }
                IInStream.SEEK_END -> {
                    channel.position(channel.size() + offset)
                }
                else -> throw SevenZipException("Invalid seekOrigin: $seekOrigin")
            }
            channel.position()
        } catch (e: IOException) {
            throw SevenZipException("Seek failed", e)
        }
    }

    @Throws(SevenZipException::class)
    override fun read(data: ByteArray): Int {
        return try {
            val buf = ByteBuffer.wrap(data)
            val read = channel.read(buf)
            if (read < 0) 0 else read
        } catch (e: IOException) {
            throw SevenZipException("Read failed", e)
        }
    }

    override fun close() {
        channel.close()
    }
}