import java.io.File

object Utils {

	fun createDirectories(path: String) {
		val directory = File(path)
		if (!directory.exists()) {
			directory.mkdirs()
		}
	}

	fun cleanupDirectory(path: String) {
		val directory = File(path)
		directory.listFiles()?.forEach { it.delete() }
	}
}