import models.RefTypeDescr
import models.Struct
import models.StructEnum
import utils.PackageConfig

abstract class KotlinGeneratorBaseStructs(pkg: PackageConfig) : KotlinGeneratorBase(pkg) {
	abstract fun writeStruct(writer: GeneratorWriter, model: Struct)
	abstract fun fileName(type: RefTypeDescr): String
	abstract fun isWriteable(type: Struct): Boolean
	open fun writeExtras() {}


	open fun writeStructs(models: List<Struct>) {
		pkg.createAndCleanupDirectory()
		models.forEach { struct ->
			try {
				if (!isWriteable(struct)) {
					return@forEach
				}
				pkg.openFile("${fileName(struct.type)}.kt").use { writer ->
					writeStruct(writer, struct)
				}
			} catch (x: Exception) {
				Exception("in model [${struct.key}]", x ).printStackTrace()
			}
			writeExtras()
		}
	}

	fun String?.convertToSingleLine(): String? {
		this ?: return null
		if (this.contains('\n')) return this.lines().joinToString("|")
		return this
	}
}