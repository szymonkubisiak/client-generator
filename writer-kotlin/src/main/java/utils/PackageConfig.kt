package utils

data class PackageConfig(
	val rootDir: Package,
	val module: Package,
	val subdirs: Package = Package.javaDefaultDir,
	val project: Package,
	val suffix: Package,
) {

	val asDir = (rootDir + module + subdirs + project + module + suffix).toDir()
	fun toDir(): String {
		return asDir
	}

	fun toPackage(): String {
		return (project + module!! + suffix).toPackage()
	}
}