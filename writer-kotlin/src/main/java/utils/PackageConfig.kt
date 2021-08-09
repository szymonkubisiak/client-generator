package utils

data class PackageConfig(
	val rootDir: Package? = null,
	val module: Package? = null,
	val subdirs: Package = Package.javaDefaultDir,
	val project: Package? = null,
	val suffix: Package? = null,
) {

	fun toDir(): String {
		return (rootDir!! + module!! + subdirs + project!! + module + suffix!!).toDir()
	}

	fun toPackage(): String {
		return (project!! + module!! + suffix!!).toPackage()
	}
}