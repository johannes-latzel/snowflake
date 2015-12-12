package snowflake.api.configuration;

/**
 * <p></p>
 * 
 * @since JDK 1.8
 * @version 2015.10.29_0
 * @author Johannes B. Latzel
 */
public interface IReadonlyStorageConfiguration extends IReadOnlyChunkManagerConfiguration, IReadOnlyFlakeManagerConfiguration {
	
	String getInitializationFilePath();
	String getDataFilePath();
	String getConfigurationFilePath();
	int getClearArraySize();
	
}
