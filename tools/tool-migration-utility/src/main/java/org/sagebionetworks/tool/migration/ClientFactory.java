package org.sagebionetworks.tool.migration;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;

/**
 * Provides a simple abstraction from client creation.
 * This enables us to unit test with mock factories.
 * @author John
 *
 */
public interface ClientFactory {

	/**
	 * Create a new client for the source repository.
	 * @return
	 * @throws SynapseException 
	 */
	public Synapse createNewSourceClient() throws SynapseException;
	
	/**
	 * Create a new client for the destination repository.
	 * @return
	 */
	public Synapse createNewDestinationClient() throws SynapseException;
}
