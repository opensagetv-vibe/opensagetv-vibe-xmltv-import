/*
 * Copyright 2016 The XMLTVImportPlugin for SageTV Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xmltv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream to filter out ascii control characters that are illegal in XML.
 * 
 * @author Demideus
 */
public class XMLInputStreamFilter extends InputStream {

	/**
	 * The original input stream.
	 */
	private final InputStream in;

	/**
	 * The substituted input stream.
	 */
	private ByteArrayInputStream bin;

	/**
	 * Creates a new XMLInputStreamFilter.
	 * 
	 * @param in the input stream to read.
	 */
	XMLInputStreamFilter(InputStream in) {
		this.in = in;
	}

	public int read() throws IOException {
		for (;;) {
			int b = nextByte();
			while (!isByteValid(b)) {
				b = nextByte();
			}
			if ('&' == b) {
				b = nextByte();
				if ('#' != b) {
					this.bin = new ByteArrayInputStream(new byte[] {(byte) b});
					return '&';
				}
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				bout.write('#');
				while (';' != (b = nextByte())) {
					// A truncated or unreasonably long character reference must not
					// spin forever and exhaust the SageTV JVM heap. Replay it as text
					// and let the XML parser report the malformed entity.
					if (b == -1) {
						this.bin = new ByteArrayInputStream(bout.toByteArray());
						return '&';
					}
					bout.write(b);
					if (bout.size() >= 64) {
						this.bin = new ByteArrayInputStream(bout.toByteArray());
						return '&';
					}
				}
				String s = bout.toString();
				try {
					if ('x' == s.charAt(1)) {
						if (!isByteValid(Integer.parseInt(s.substring(2), 16))) {
							continue;
						}
					} else {
						if (!isByteValid(Integer.parseInt(s.substring(1)))) {
							continue;
						}
					}
				} catch (NumberFormatException e) {
					// Not a valid charref! Let xerces deal with it.
				}
				bout.write(';');
				this.bin = new ByteArrayInputStream(bout.toByteArray());
				return '&';
			}
			return b;
		}
	}

	/**
	 * Checks the validity of a byte. Assumes an 8-bit encoding.
	 * 
	 * @param b the byte.
	 * @return <code>true</code>if the byte is valid.
	 */
	private boolean isByteValid(int b) {
		return -1 == b || 9 == b || 0xa == b || 0xd == b || 0x20 <= b;
	}

	/**
	 * Reads the next byte from either the byte array or the original input
	 * stream.
	 * 
	 * @return the next byte. -1 when at end of stream.
	 * @throws IOException if an I/O error occurs.
	 */
	private int nextByte() throws IOException {
		if (this.bin != null) {
			int b = this.bin.read();
			if (b != -1) {
				return b;
			}
			this.bin = null;
		}
		return this.in.read();
	}
}
