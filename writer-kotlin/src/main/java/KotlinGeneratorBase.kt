import models.*
import utils.PackageConfig
import java.io.PrintWriter

abstract class KotlinGeneratorBase(
	val pkg: PackageConfig,
	protected val typeResolver: TypeResolver = TypeResolver.instance,
) {
	abstract fun writeStruct(writer: GeneratorWriter, model: Struct)
	abstract fun fileName(type: RefTypeDescr): String
	open fun isWriteable(type: Struct) = true
	open fun writeExtras() {}


	open fun writeStructs(models: List<Struct>) {
		pkg.createAndCleanupDirectory()
		models.forEach { struct ->
			if (!isWriteable(struct)) {
				return@forEach
			}
			pkg.openFile("${fileName(struct.type)}.kt").use { writer ->
				writeStruct(writer, struct)
			}
		}
		writeExtras()
	}

	fun PackageConfig.createDirectory() {
		Utils.createDirectories(this.asDir)
	}

	fun PackageConfig.cleanupDirectory() {
		Utils.cleanupDirectory(this.asDir)
	}

	fun PackageConfig.createAndCleanupDirectory() {
		createDirectory()
		cleanupDirectory()
	}

	fun PackageConfig.openFile(fileName: String): BaseWriter {
		val outerWriter = PrintWriter("$asDir/$fileName")
		val retval = BaseWriter(outerWriter)
		return retval
	}
}
