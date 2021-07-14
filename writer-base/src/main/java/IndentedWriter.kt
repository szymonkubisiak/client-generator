import java.io.Closeable

class IndentedWriter (private val out: GeneratorWriter, private val indent: String = "\t" ) : GeneratorWriter, Closeable {
	override fun writeLine(arg: String) {
		out.writeLine(indent + arg)
	}

	override fun close() {
		//nuttink!
	}
}