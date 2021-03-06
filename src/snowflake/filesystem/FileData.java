package snowflake.filesystem;

import java.nio.ByteBuffer;

import j3l.util.Checker;
import snowflake.GlobalString;
import snowflake.StaticMode;

/**
 * <p></p>
 * 
 * @since JDK 1.8
 * @version 2016.09.30_0
 * @author Johannes B. Latzel
 */
public final class FileData extends NodeData {
	
	
	/**
	 * <p></p>
	 */
	public final static int FILE_DATA_LENGTH = 25;
	
	
	/**
	 * <p></p>
	 */
	private final static int ATTRIBUTE_FLAKE_IDENTIFICATION_POSITION = 0;
	
	
	/**
	 * <p></p>
	 */
	private final static int DATA_FLAKE_IDENTIFICATION_POSITION = 8;
	
	
	/**
	 * <p></p>
	 */
	private final static int PARENT_DIRECTORY_IDENTIFICATION_POSITION = 16;
	
	
	/**
	 * <p></p>
	 */
	private final static int FLAG_VECTOR_POSITION = 24;
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public static ByteBuffer getBinaryData(ByteBuffer buffer, long attribute_flake_identification,
			long data_flake_identification, long parent_directory_identification, boolean is_empty) {
		if( StaticMode.TESTING_MODE ) {
			Checker.checkForNull(buffer, GlobalString.Buffer.toString());
		}
		Checker.checkForBoundaries(
			buffer.remaining(),
			FileData.FILE_DATA_LENGTH,
			FileData.FILE_DATA_LENGTH,
			GlobalString.BufferLength.toString()
		);
		buffer.putLong(FileData.ATTRIBUTE_FLAKE_IDENTIFICATION_POSITION, attribute_flake_identification);
		buffer.putLong(FileData.DATA_FLAKE_IDENTIFICATION_POSITION, data_flake_identification);
		buffer.putLong(FileData.PARENT_DIRECTORY_IDENTIFICATION_POSITION, parent_directory_identification);
		buffer.put(FileData.FLAG_VECTOR_POSITION, (byte)( is_empty ? 1 : 0 ));
		return buffer;
		
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public static ByteBuffer createBuffer() {
		return ByteBuffer.allocate(FileData.FILE_DATA_LENGTH);
	}
	
	
	/**
	 * <p></p>
	 */
	private final long attribute_flake_identification;
	
	
	/**
	 * <p></p>
	 */
	private final long data_flake_identification;
	
	
	/**
	 * <p></p>
	 */
	private final long parent_directory_identification;
	
	
	/**
	 * <p></p>
	 */
	private final boolean is_empty;
	
	
	/**
	 * <p></p>
	 * 
	 * @param
	 */
	public FileData(ByteBuffer buffer, long index) {
		super(index);
		if( StaticMode.TESTING_MODE ) {
			Checker.checkForNull(buffer, GlobalString.Buffer.toString());
		}
		Checker.checkForBoundaries(
			buffer.remaining(), getDataLength(), Integer.MAX_VALUE, GlobalString.BufferLength.toString()
		);
		attribute_flake_identification = buffer.getLong(FileData.ATTRIBUTE_FLAKE_IDENTIFICATION_POSITION);
		data_flake_identification = buffer.getLong(FileData.DATA_FLAKE_IDENTIFICATION_POSITION);
		parent_directory_identification = buffer.getLong(FileData.PARENT_DIRECTORY_IDENTIFICATION_POSITION);
		is_empty = buffer.get(FileData.FLAG_VECTOR_POSITION) == 1;
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public long getAttributeFlakeIdentification() {
		return attribute_flake_identification;
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public long getDataFlakeIdentification() {
		return data_flake_identification;
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public long getParentDirectoryIdentification() {
		return parent_directory_identification;
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public boolean isEmpty() {
		return is_empty;
	}
	
	
	/* (non-Javadoc)
	 * @see snowflake.core.data.IBinaryData#getBinaryData(byte[])
	 */
	@Override public void getBinaryData(byte[] buffer) {
		FileData.getBinaryData(
			ByteBuffer.wrap(buffer),
			attribute_flake_identification,
			data_flake_identification,
			parent_directory_identification,
			is_empty
		);
	}
	
	
	/* (non-Javadoc)
	 * @see snowflake.core.data.IBinaryData#getDataLength()
	 */
	@Override public int getDataLength() {
		return FileData.FILE_DATA_LENGTH;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override public String toString() {
		StringBuilder string_builder = new StringBuilder(110);
		string_builder.append("FileData: [attribute_flake_identification = ");
		string_builder.append(attribute_flake_identification);
		string_builder.append(" | data_flake_identification = ");
		string_builder.append(data_flake_identification);
		string_builder.append(" | parent_directory_identification = ");
		string_builder.append(parent_directory_identification);
		string_builder.append(" | is_empty = ");
		string_builder.append(is_empty);
		string_builder.append("]");
		return string_builder.toString();
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override public boolean equals(Object object) {
		if( object != null && object instanceof FileData ) {
			FileData file_data = (FileData)object;
			if( file_data.hashCode() == hashCode() ) {
				return file_data.attribute_flake_identification == attribute_flake_identification
						&& file_data.data_flake_identification == data_flake_identification
						&& file_data.parent_directory_identification == parent_directory_identification
						&& file_data.is_empty == is_empty;
			}
		}
		return false;
	}
	
}
