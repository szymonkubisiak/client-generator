import java.io.Writer

class BaseWriter(private val out: Writer): GeneratorWriter, AutoCloseable {
	override fun writeLine(arg: String) {
		out.write("$arg\n")
	}

	override fun close() {
		out.close()
	}
}