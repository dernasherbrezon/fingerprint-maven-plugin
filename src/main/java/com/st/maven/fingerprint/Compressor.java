package com.st.maven.fingerprint;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.maven.plugin.logging.Log;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

class Compressor {

	static String compressJavaScript(Reader in, final Log log) {
		JavaScriptCompressor compressor;
		try {
			compressor = new JavaScriptCompressor(in, new ErrorReporter() {

				@Override
				public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
					if (line < 0) {
						log.warn(message);
					} else {
						log.warn(line + ':' + lineOffset + ':' + message);
					}
				}

				@Override
				public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
					if (line < 0) {
						log.error(message);
					} else {
						log.error(line + ':' + lineOffset + ':' + message);
					}
				}

				@Override
				public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
					error(message, sourceName, line, lineSource, lineOffset);
					return new EvaluatorException(message);
				}
			});
			try (StringWriter w = new StringWriter()) {
				compressor.compress(w, -1, true, true, false, false);
				w.flush();
				return w.toString();
			}
		} catch (Exception e) {
			throw new RuntimeException("unable to process", e);
		} finally {
			// Close the input stream first, and then open the output stream,
			// in case the output file should override the input file.
			try {
				in.close();
			} catch (IOException e) {
				log.error("unable to close cursor", e);

			}
			in = null;
		}
	}

	static String compressCSS(Reader in, final Log log) {

		CssCompressor compressor;
		try {
			compressor = new CssCompressor(in);
			try (StringWriter w = new StringWriter()) {
				compressor.compress(w, -1);
				w.flush();
				return w.toString();
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to process", e);
		} finally {
			// Close the input stream first, and then open the output stream,
			// in case the output file should override the input file.
			try {
				in.close();
			} catch (IOException e) {
				log.error("unnable to close cursors", e);
			}
			in = null;
		}
	}

	private Compressor() {
		//do nothing
	}
}
