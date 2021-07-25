import Namer.domainFinalName
import Namer.transportFinalName
import Namer.adapterT2DName
import models.*
import java.io.PrintWriter
import java.io.Writer

abstract class KotlinGeneratorBase(protected val typeResolver: TypeResolver = TypeResolver()) {
	abstract fun writeStruct(writer: GeneratorWriter, model: Struct)
	abstract fun writeField(writer: GeneratorWriter, field: Field)

	fun writeStructs(models: List<Struct>, directory: String) {
		Utils.createDirectories(directory)
		Utils.cleanupDirectory(directory)
		models.forEach { struct ->
			PrintWriter("$directory/${struct.transportName}.kt").use { writer ->
				writeStruct(BaseWriter(writer), struct)
				writer.flush()
			}
		}
	}
}
