import models.*
import java.io.PrintWriter

abstract class KotlinGeneratorBase(protected val typeResolver: TypeResolver = TypeResolver()) {
	abstract fun writeStruct(writer: GeneratorWriter, model: Struct)
	abstract fun writeField(writer: GeneratorWriter, field: Field)
	abstract fun fileName(type: TypeDescr): String


	fun writeStructs(models: List<Struct>, directory: String) {
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
