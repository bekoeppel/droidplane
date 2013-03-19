package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;

import android.util.Log;


/**
 * TODO: documentation in the whole file!
 */
class RandomAccessFileReader extends Reader {
	
	private RandomAccessFile randomAccessFile;

	/*
	 * (non-Javadoc) the SAX parser is calling close() when it has finished
	 * the parsing. We don't want that. To close the file, we provide
	 * another method close(boolean force).
	 * 
	 * @see java.io.Reader#close()
	 */
	@Override
	public void close() throws IOException {
		Log.d(MainApplication.TAG, "RandomAccessFileReader close() called - ignoring");
		//randomAccessFile.close();
	}
	
	public void close(boolean force) throws IOException {
		Log.d(MainApplication.TAG, "RandomAccessFileReader close(force=true) called - closing");
		randomAccessFile.close();
	}

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
	
	public RandomAccessFileReader(RandomAccessFile randomAccessFile) {
		this.randomAccessFile = randomAccessFile;
	}		
}
