package utils

class Package(vararg arr: String) {
	val list = arr.asList()
	operator fun plus(suffix: Package): Package {
		return Package(*(this.list + suffix.list).toTypedArray())
	}

	fun toPackage(): String {
		return list.joinToString(".")
	}

	fun toDir(): String {
		return list.joinToString("/")
	}

	companion object {
		val javaDefaultDir = Package("src", "main", "java")
		val dummy = Package()
	}
}