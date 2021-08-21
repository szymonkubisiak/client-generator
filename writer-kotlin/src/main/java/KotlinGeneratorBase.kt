import models.*
import utils.PackageConfig
import java.io.PrintWriter

abstract class KotlinGeneratorBase(
	val pkg: PackageConfig,
	protected val typeResolver: TypeResolver = TypeResolver.instance,
) {
	abstract fun writeStruct(writer: GeneratorWriter, model: Struct)
	//abstract fun writeField(writer: GeneratorWriter, field: Field)
	abstract fun fileName(type: StructTypeDescr): String
	open fun isWriteable(type: Struct) = true
	open fun writeExtras(directory: String) {}


	open fun writeStructs(models: List<Struct>, cleanDirectory: Boolean = true) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)
		if(cleanDirectory) Utils.cleanupDirectory(directory)
		models.forEach { struct ->
			if (!isWriteable(struct)) {
				return@forEach
			}
			PrintWriter("$directory/${fileName(struct.type)}.kt").use { writer ->
				writeStruct(BaseWriter(writer), struct)
				writer.flush()
			}
		}
		writeExtras(directory)
	}
}
