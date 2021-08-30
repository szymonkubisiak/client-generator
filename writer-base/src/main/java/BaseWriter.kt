import java.io.Closeable
import java.io.Writer

class BaseWriter(private val out: Writer): GeneratorWriter, Closeable {
	override fun writeLine(arg: String) {
		out.write("$arg\n")
	}

	override fun close() {
		out.flush()
		out.close()
	}
}