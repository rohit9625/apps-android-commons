package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface.TAG_ARTIST
import androidx.exifinterface.media.ExifInterface.TAG_BODY_SERIAL_NUMBER
import androidx.exifinterface.media.ExifInterface.TAG_CAMERA_OWNER_NAME
import androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT
import androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE
import androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF
import androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE
import androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF
import androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE
import androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF
import androidx.exifinterface.media.ExifInterface.TAG_LENS_MAKE
import androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL
import androidx.exifinterface.media.ExifInterface.TAG_LENS_SERIAL_NUMBER
import androidx.exifinterface.media.ExifInterface.TAG_LENS_SPECIFICATION
import androidx.exifinterface.media.ExifInterface.TAG_MAKE
import androidx.exifinterface.media.ExifInterface.TAG_MODEL
import androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Arrays

/**
 * Test cases for FileMetadataUtils
 */
class FileMetadataUtilsTest {
    /**
     * Test method to verify EXIF tags for "Author"
     */
    @Test
    fun getTagsFromPrefAuthor() {
        val author = FileMetadataUtils.getTagsFromPref("Author")
        val authorRef = arrayOf(TAG_ARTIST, TAG_CAMERA_OWNER_NAME)

        assertTrue(Arrays.deepEquals(author, authorRef))
    }

    /**
     * Test method to verify EXIF tags for  "Location"
     */
    @Test
    fun getTagsFromPrefLocation() {
        val author = FileMetadataUtils.getTagsFromPref("Location")
        val authorRef =
            arrayOf(
                TAG_GPS_LATITUDE,
                TAG_GPS_LATITUDE_REF,
                TAG_GPS_LONGITUDE,
                TAG_GPS_LONGITUDE_REF,
                TAG_GPS_ALTITUDE,
                TAG_GPS_ALTITUDE_REF,
            )

        assertTrue(Arrays.deepEquals(author, authorRef))
    }

    /**
     * Test method to verify EXIF tags for  "Copyright"
     */
    @Test
    fun getTagsFromPrefCopyWright() {
        val author = FileMetadataUtils.getTagsFromPref("Copyright")
        val authorRef = arrayOf(TAG_COPYRIGHT)

        assertTrue(Arrays.deepEquals(author, authorRef))
    }

    /**
     * Test method to verify EXIF tags for  "Camera Model"
     */
    @Test
    fun getTagsFromPrefCameraModel() {
        val author = FileMetadataUtils.getTagsFromPref("Camera Model")
        val authorRef = arrayOf(TAG_MAKE, TAG_MODEL)

        assertTrue(Arrays.deepEquals(author, authorRef))
    }

    /**
     * Test method to verify EXIF tags for  "Lens Model"
     */
    @Test
    fun getTagsFromPrefLensModel() {
        val author = FileMetadataUtils.getTagsFromPref("Lens Model")
        val authorRef = arrayOf(TAG_LENS_MAKE, TAG_LENS_MODEL, TAG_LENS_SPECIFICATION)

        assertTrue(Arrays.deepEquals(author, authorRef))
    }

    /**
     * Test method to verify EXIF tags for  "Serial Numbers"
     */
    @Test
    fun getTagsFromPrefSerialNumbers() {
        val author = FileMetadataUtils.getTagsFromPref("Serial Numbers")
        val authorRef = arrayOf(TAG_BODY_SERIAL_NUMBER, TAG_LENS_SERIAL_NUMBER)

        assertTrue(Arrays.deepEquals(author, authorRef))
    }

    /**
     * Test method to verify EXIF tags for  "Software"
     */
    @Test
    fun getTagsFromPrefSoftware() {
        val author = FileMetadataUtils.getTagsFromPref("Software")
        val authorRef = arrayOf(TAG_SOFTWARE)

        assertTrue(Arrays.deepEquals(author, authorRef))
    }
}
