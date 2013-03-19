package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;

import android.util.Log;


/**
 * The RandomAccessFileReader wraps the RandomAccessFile into a Reader format.
 * This is requried because the SAX parser can not directly read from a
 * RandomAccessFile, but from a Reader. Because the RandomAccessFile is byte
 * based, but the Reader is char based, the RandomAccessFileReader has to take
 * care of the document encoding and translate the bytes into characters. For my
 * mind map, the encoding was set to US-ASCII, so I used this encoding for the
 * moment. This class only supports reading from the file, but not writing (it's
 * a Reader after all).
 * 
 * TODO: make this class more generic, by allowing the user to set the encoding.
 * Then we could even reuse it in different projects. Also, we should try to
 * find the encoding automatically by looking at the file.
 */
class RandomAccessFileReader extends Reader {
	
	/**
	 * The RandomAccessFile we're reading from.
	 */
	private RandomAccessFile randomAccessFile;

	/**
	 * Create a Reader based on a RandomAccessFile
	 * @param randomAccessFile
	 */
	public RandomAccessFileReader(RandomAccessFile randomAccessFile) {
		this.randomAccessFile = randomAccessFile;
	}
	
	// TODO: currently, the whole application uses the RandomAccessFile when it
	// wants, and uses the RandomAccessFileReader when there's no way around it.
	// Maybe we should start using the RandomAccessFileReader everywhere, but
	// implement seek/rewind functionality here?
	
	/*
	 * (non-Javadoc) I have noticed that the SAX parser is calling close() when
	 * it has finished the parsing. We don't want that. To close the file, we
	 * provide another method close(boolean force).
	 * 
	 * TODO: instead of changing the default behaviour, we should add a "lock"
	 * method disallowClosing(). If that method was called, then close() does
	 * nothing, otherwise close() should do it's intended job.
	 * 
	 * @see java.io.Reader#close()
	 */
	@Override
	public void close() throws IOException {
		Log.d(MainApplication.TAG, "RandomAccessFileReader close() called - ignoring");
	}
	
	/**
	 * This method is a replacement for {@link RandomAccessFileReader#close()},
	 * where we have disabled the actual closing of the RandomAccessFile. Note,
	 * even though this close() method accepts a force flag, it does not matter
	 * whether the flag is true or false - the file will be closed in either
	 * case.
	 * 
	 * TODO: maybe we should give the force flag a meaning
	 * 
	 * @param force
	 * @throws IOException
	 */
	public void close(boolean force) throws IOException {
		Log.d(MainApplication.TAG, "RandomAccessFileReader close(force=true) called - closing");
		randomAccessFile.close();
	}

	// TODO: in order to make this class generic and reusable in other projects,
	// we should overwrite all methods from the Reader. Especially the readChar
	// is certainly something we want to implement as well.
	
	/*
	 * (non-Javadoc) Read bytes from the RandomAccessFile, converts the bytes
	 * into characters (by using US-ASCII encoding) and returns the read
	 * characters.
	 * 
	 * @see java.io.Reader#read(char[], int, int)
	 */
	@Override
	public int read(char[] buffer, int byteOffset, int byteCount) throws IOException {
		
		// first read into a byteBuffer
		byte[] byteBuffer = new byte[byteCount];
		int numBytesRead = randomAccessFile.read(byteBuffer, byteOffset, byteCount);

		// check if we have reached the end of the file
		if (numBytesRead == -1) {
			return -1;
		}
		
		// translate the read bytes into characters
		else {
			
			// the input file has US-ASCII encoding, and we transfer all characters into the buffer
			new String(byteBuffer, "US-ASCII").getChars(0, numBytesRead, buffer, 0);

			// return the number of bytes read
			return numBytesRead;
		}
	}
}
