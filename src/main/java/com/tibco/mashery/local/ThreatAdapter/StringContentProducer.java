package com.tibco.mashery.local.ThreatAdapter;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.mashery.http.io.ContentProducer;

/**
 * Content producer for String payloads.
 */
public class StringContentProducer implements ContentProducer {

	private static final int MAX_FIXED_LENGTH = 4096;

	protected static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	protected final String content;

	protected final Charset charset;

	private byte[] buffer;

	private long contentLength = -2L;

	/**
	 * @param content string content to output
	 * @param charset desired output charset
	 */
	public StringContentProducer(String content, Charset charset) {
		this.content = content;
		if(charset != null){
			this.charset = charset;
		}else{
			this.charset = Charset.defaultCharset();
		}
	}

	public StringContentProducer(String content) {
		this(content, DEFAULT_CHARSET);
	}

	private void initialize() {
		if(content == null){
			return;
		}
		if (contentLength == -2L) {
			String content = this.content;
			int len = content.length();
			if (len > MAX_FIXED_LENGTH) {
				// we'll chunk
				contentLength = -1L;
			} else {
				// small enough to buffer ahead
				byte[] buffer = content.getBytes(charset);
				contentLength = buffer.length;
				this.buffer = buffer;
			}
		}
	}

	public long getContentLength() {
		initialize();
		return contentLength;
	}

	public void writeTo(OutputStream out) throws IOException {
		initialize();
		byte[] buffer = this.buffer;
		if (buffer == null) {
			String content = this.content;
			int chunkSize = MAX_FIXED_LENGTH;
			Charset charset = this.charset;
			if(content !=null){
				for (int i = 0, n = content.length(); i < n; i += chunkSize) {
					int end = Math.min(n, i + chunkSize);
					String str = content.substring(i, end);
					out.write(str.getBytes(charset));
				}
			}else{
				throw new IOException("No content to write");
			}
		} else {
			out.write(buffer);
		}
	}

	public boolean isRepeatable() {
		return true;
	}
}
