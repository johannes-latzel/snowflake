package snowflake.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import j3l.util.Checker;
import j3l.util.IBinaryData;
import j3l.util.IDataTable;
import j3l.util.Indexable;
import j3l.util.LongRange;
import snowflake.GlobalString;
import snowflake.Util;
import snowflake.api.DataPointer;
import snowflake.api.IFlake;
import snowflake.core.FlakeInputStream;
import snowflake.core.FlakeOutputStream;


/**
 * <p></p>
 * 
 * @since JDK 1.8
 * @version 2016.09.30_0
 * @author Johannes B. Latzel
 */
public abstract class FileSystemDataTable<T extends Indexable, R extends IBinaryData> implements IDataTable<T, R> {
	
	
	/**
	 * <p></p>
	 */
	protected final ArrayList<LongRange> available_index_list;
	
	
	/**
	 * <p></p>
	 */
	protected final FlakeInputStream flake_input_stream;
	
	
	/**
	 * <p></p>
	 */
	protected final FlakeOutputStream flake_output_stream;
	
	
	/**
	 * <p></p>
	 */
	protected final ByteBuffer clear_buffer;
	
	
	/**
	 * <p></p>
	 * 
	 * @param
	 */
	protected FileSystemDataTable(IFlake table_flake, int data_entry_size) throws IOException {
		Checker.checkForNull(table_flake, GlobalString.TableFlake.toString());
		flake_input_stream = table_flake.getFlakeInputStream();
		flake_output_stream = table_flake.getFlakeOutputStream();
		available_index_list = new ArrayList<>(1000);
		clear_buffer = ByteBuffer.allocateDirect(
			Checker.checkForBoundaries(data_entry_size, 1, Integer.MAX_VALUE, GlobalString.DataEntrySize.toString())
		);
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	protected final void addAvailableIndex(long index) {
		synchronized( available_index_list ) {
			for( LongRange range : available_index_list ) {
				if( range.elementIsAddable(index) ) {
					range.add(index);
					return;
				}
			}
			available_index_list.add(new LongRange(index));
		}
	}
	
	
	/* (non-Javadoc)
	 * @see j3l.util.DataTable#trim()
	 */
	@Override public final void trim() throws IOException {
		synchronized( available_index_list ) {
			available_index_list.sort(LongRange.BY_END_COMPARATOR);
			synchronized( flake_input_stream ) {
				synchronized( flake_output_stream ) {
					DataPointer pointer = flake_output_stream.getDataPointer();
					pointer.seekEOF();
					clear_buffer.rewind();
					int data_length = clear_buffer.capacity();
					ByteBuffer buffer = FileData.createBuffer();
					while( pointer.getPositionInFlake() != 0 ) {
						pointer.changePosition(-data_length);
						long current_position_in_flake = pointer.getPositionInFlake();
						Util.readComplete(flake_input_stream, buffer);
						if( !Checker.checkAllElements(buffer, (byte)0) ) {
							long current_range_position;
							LongRange current_range;
							for(int a=available_index_list.size()-1;a>=0;a--) {
								current_range = available_index_list.get(a);
								while( current_range.isValid() ) {
									current_range_position = current_range.getEnd() * data_length;
									if( current_range_position >= current_position_in_flake ) {
										current_range.reduceEnd();
									}
									else {
										break;
									}
								}
							}
							flake_output_stream.trim();
							return;
						}
						pointer.changePosition(-data_length);
					}
					flake_output_stream.ensureRemainingCapacity(100 * data_length);
					pointer.seekEOF();
				}
			}
		}
	}
	
	
	/* (non-Javadoc)
	 * @see j3l.util.DataTable#getAvailableIndex()
	 */
	@Override public final long getAvailableIndex() throws IOException {
		synchronized( available_index_list ) {
			available_index_list.sort(LongRange.BY_BEGIN_COMPARATOR.reversed());
			LongRange range;
			for(int a=available_index_list.size()-1;a>=0;a--) {
				range = available_index_list.get(a);
				if( !range.isValid() ) {
					available_index_list.remove(a);
					continue;
				}
				return range.increaseBegin();
			}
			long begin;
			long end;
			synchronized( flake_input_stream ) {
				synchronized( flake_output_stream ) {
					DataPointer pointer = flake_output_stream.getDataPointer();
					int data_length = clear_buffer.capacity();
					begin = pointer.getFlakeLength() / data_length;
					pointer.seekEOF();
					flake_output_stream.ensureRemainingCapacity(100 * data_length);
					end = pointer.getFlakeLength() / data_length;
				}
			}
			range = new LongRange(begin, end);
			available_index_list.add(range);
			return range.increaseBegin();
		}
	}
	
}
