package snowflake.core.storage;

import java.io.IOException;

import j3l.util.Checker;
import snowflake.GlobalString;
import snowflake.StaticMode;
import snowflake.api.DataPointer;
import snowflake.core.Returnable;

/**
 * <p></p>
 * 
 * @since JDK 1.8
 * @version 2016.07.11_0
 * @author Johannes B. Latzel
 */
public interface IWrite extends Returnable {

	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	void write(DataPointer data_pointer, byte b) throws IOException;
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	default void write(DataPointer data_pointer, byte[] buffer) throws IOException {
		if( StaticMode.TESTING_MODE ) {
			Checker.checkForNull(buffer, GlobalString.Buffer.toString());
		}
		if( buffer.length == 0 ) {
			return;
		}
		write(data_pointer, buffer, 0, buffer.length);
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	void write(DataPointer data_pointer, byte[] buffer, int offset, int length) throws IOException;
	
}
