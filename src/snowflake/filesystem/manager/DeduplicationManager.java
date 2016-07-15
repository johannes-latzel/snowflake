package snowflake.filesystem.manager;

import java.io.IOException;
import java.util.ArrayList;

import j3l.util.Checker;
import j3l.util.ClosureState;
import j3l.util.IClose;
import j3l.util.InputUtility;
import j3l.util.LoopedTaskThread;
import j3l.util.TransformValue2;
import snowflake.GlobalString;
import snowflake.StaticMode;
import snowflake.api.DataPointer;
import snowflake.api.FileSystemException;
import snowflake.api.FlakeInputStream;
import snowflake.api.FlakeOutputStream;
import snowflake.api.IDirectory;
import snowflake.api.IFlake;
import snowflake.core.storage.ICreateFlake;
import snowflake.filesystem.File;
import snowflake.filesystem.FileSystem;
import snowflake.filesystem.Lock;
import snowflake.filesystem.Node;

/**
 * <p></p>
 * 
 * @since JDK 1.8
 * @version 2016.07.15_0
 * @author Johannes B. Latzel
 */
public final class DeduplicationManager implements IClose<FileSystemException> {
	
	
	/**
	 * <p></p>
	 */
	private final LoopedTaskThread deduplication_thread;
	
	
	/**
	 * <p></p>
	 */
	private final LoopedTaskThread analyzation_thread;
	
	
	/**
	 * <p></p>
	 */
	private final FileSystem file_system;
	
	
	/**
	 * <p></p>
	 */
	private final ArrayList<File> deduplicant_list;
	
	
	/**
	 * <p></p>
	 */
	private final DeduplicationTable deduplication_table;
	
	
	/**
	 * <p></p>
	 */
	private ClosureState closure_state;
	
	
	/**
	 * <p></p>
	 */
	private final ICreateFlake create_flake;
	
	
	/**
	 * <p></p>
	 * 
	 * @param
	 */
	public DeduplicationManager(FileSystem file_system, IFlake deduplication_table_flake, ICreateFlake create_flake) {
		if( StaticMode.TESTING_MODE ) {
			this.file_system = Checker.checkForNull(file_system, GlobalString.FileSystem.toString());
			this.create_flake = Checker.checkForNull(create_flake, GlobalString.CreateFlake.toString());
		}
		else {
			this.file_system = file_system;
			this.create_flake = create_flake;
		}
		deduplicant_list = new ArrayList<>();
		try {
			deduplication_table = new DeduplicationTable(deduplication_table_flake);
		}
		catch( IOException e ) {
			throw new FileSystemException("Can not instantiate the deduplication_table!", e);
		}
		this.deduplication_thread = new LoopedTaskThread(
			this::deduplicateFile,
			"Snowflake Deduplication Thread",
			1_000
		);
		this.analyzation_thread = new LoopedTaskThread(
			this::analyzeFileSystem,
			"Snowflake Deduplication Analyzation Thread",
			1_000_000
		);
		this.closure_state = ClosureState.None;
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	private void deduplicateFile() {
		File file;
		synchronized( deduplicant_list ) {
			while( deduplicant_list.isEmpty() ) {
				try {
					deduplicant_list.wait();
				}
				catch( InterruptedException e ) {
					e.printStackTrace();
				}
			}
			file = deduplicant_list.remove(deduplicant_list.size() - 1);
		}
		deduplicateFile(file);
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	private void analyzeFileSystem() {
		ArrayList<File> new_deduplicant_list = new ArrayList<>(10_000);
		ArrayList<IDirectory> next_directory_list = new ArrayList<>(10_000);
		ArrayList<Node> next_child_node_list = new ArrayList<>(1_000);
		next_child_node_list.addAll(file_system.getRootDirectory().getChildNodes());
		Node current_node;
		File current_file;
		while( !next_directory_list.isEmpty()  ) {
			if( next_child_node_list.isEmpty() ) {
				next_child_node_list.addAll(
					next_directory_list.remove(next_directory_list.size() - 1).getChildNodes()
				);
				// continue is import, because the removal in the below code could cause a crash
				continue;
			}
			current_node = next_child_node_list.remove(next_child_node_list.size() - 1);
			if( current_node instanceof IDirectory ) {
				// cast is okay, because current_node is an instance of IDirectory
				next_directory_list.add((IDirectory)current_node);
			}
			else if( current_node instanceof File ) {
				// cast is okay, because current_node is an instance of File
				current_file = (File)current_node;
				if( isPotentialDeduplicant(current_file) ) {
					new_deduplicant_list.add(current_file);
				}
			}
		}
		if( !new_deduplicant_list.isEmpty() ) {
			synchronized( deduplicant_list ) {
				deduplicant_list.addAll(new_deduplicant_list);
				deduplicant_list.notifyAll();
			}
		}
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public boolean isPotentialDeduplicant(File file) {
		// assumes all files are okay
		return file != null && deduplication_table != null;
	}
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	public void deduplicateFile(File file) {
		if( StaticMode.TESTING_MODE ) {
			Checker.checkForValidation(file, GlobalString.File.toString());
		}
		Lock lock = file.lock();
		IDeduplicationDescription deduplication_description = file.getDeduplicationDescription();
		byte new_deduplication_level = (byte)(deduplication_description.getDeduplicationLevel() + 1);
		if( new_deduplication_level < 0 ) {
			throw new FileSystemException("Can not deduplicate a file more than 127 times!");
		}
		long end_of_dedupublication_pointer = 0;
		IFlake deduplication_flake = create_flake.createFlake();
		byte[] current_data_block = new byte[DeduplicationBlock.SIZE];
		byte[] long_buffer = new byte[Long.BYTES];
		long current_index;
		try( FlakeInputStream fin = file.getFlakeInputStream() ) {
			DataPointer data_pointer = fin.getDataPointer();
			try( FlakeOutputStream fout = deduplication_flake.getFlakeOutputStream() ) {
				while( data_pointer.getRemainingBytes() >= DeduplicationBlock.SIZE ) {
					InputUtility.readComplete(fin, current_data_block);
					if( !deduplication_table.isRegistered(current_data_block) ) {
						current_index = deduplication_table.register(current_data_block);
					}
					else {
						current_index = deduplication_table.getIndex(current_data_block);
					}
					fout.write(TransformValue2.toByteArray(current_index, long_buffer));
					end_of_dedupublication_pointer += long_buffer.length;
				}
				// cast is okay, because data_pointer.getRemainingBytes() is file
				byte[] remaining_data_buffer = new byte[(int)(data_pointer.getRemainingBytes())];
				InputUtility.readComplete(fin, remaining_data_buffer);
			}
			catch( IOException e ) {
				throw new IOException("Could not write to the deduplication_flake!", e);
			}
		}
		catch( IOException e ) {
			// delete flake, because it is no longer needed
			deduplication_flake.delete();
			throw new FileSystemException("Can not deduplicate the file \"" + file.toString() + "!", e);
		}
	}
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	private void dededuplicateFile(File file) {
		
	}
	
	
	/* (non-Javadoc)
	 * @see j3l.util.close.IStateClosure#getClosureState()
	 */
	@Override public ClosureState getClosureState() {
		return closure_state;
	}
	
	
	/* (non-Javadoc)
	 * @see j3l.util.close.IClose#open()
	 */
	@Override public synchronized void open() {
		if( isOpen() ) {
			if( StaticMode.TESTING_MODE ) {
				throw new FileSystemException("The DeduplicationManager has already been opened!");
			}
			return;
		}
		closure_state = ClosureState.InOpening;
		analyzation_thread.start();
		deduplication_thread.start();
		closure_state = ClosureState.Open;
	}
	
	
	/* (non-Javadoc)
	 * @see j3l.util.close.IClose#close()
	 */
	@Override public synchronized void close() {
		if( !isOpen() ) {
			if( StaticMode.TESTING_MODE ) {
				throw new FileSystemException("The DeduplicationManager is not open!");
			}
			return;
		}
		closure_state = ClosureState.InClosure;
		analyzation_thread.interrupt();
		deduplication_thread.interrupt();
		closure_state = ClosureState.Closed;
	}
	
}
