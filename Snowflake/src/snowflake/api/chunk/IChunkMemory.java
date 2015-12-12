package snowflake.api.chunk;

import snowflake.core.Chunk;
import snowflake.core.flake.Flake;


/**
 * <p></p>
 * 
 * @since JDK 1.8
 * @version 2015.11.27_0
 * @author Johannes B. Latzel
 */
public interface IChunkMemory {
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	void deleteChunk(Chunk chunk);
	
	
	/**
	 * <p></p>
	 *
	 * @param
	 * @return
	 */
	void saveChunk(Flake owner_flake, Chunk chunk);
	
}
