import models.*
import utils.PackageConfig
import java.io.PrintWriter

abstract class KotlinGeneratorBase(
	val pkg: PackageConfig,
	protected val typeResolver: TypeResolver = TypeResolver.instance,
) {
	abstract fun writeStruct(writer: GeneratorWriter, model: Struct)
	abstract fun writeField(writer: GeneratorWriter, field: Field)
	abstract fun fileName(type: TypeDescr): String


	fun writeStructs(models: List<Struct>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)
		Utils.cleanupDirectory(directory)
		models.forEach { struct ->
			PrintWriter("$directory/${fileName(struct.type)}.kt").use { writer ->
				writeStruct(BaseWriter(writer), struct)
				writer.flush()
			}
		}
	}
}
