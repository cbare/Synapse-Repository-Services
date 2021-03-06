package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.sagebionetworks.repo.model.table.AsynchDownloadFromTableResponseBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.csv.CSVWriterStream;

/**
 * Abstraction for Table Row management.
 * 
 * @author jmhill
 * 
 */
public interface TableRowManager {

	/**
	 * Append a set of rows to a table.
	 * 
	 * @param user
	 * @param delta
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws IOException
	 */
	public RowReferenceSet appendRows(UserInfo user, String tableId,
			List<ColumnModel> models, RowSet delta) throws DatastoreException,
			NotFoundException, IOException;

	/**
	 * Append or update a set of partial rows to a table.
	 * 
	 * @param user
	 * @param tableId
	 * @param models
	 * @param rowsToAppendOrUpdate
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public RowReferenceSet appendPartialRows(UserInfo user, String tableId, List<ColumnModel> models, PartialRowSet rowsToAppendOrUpdate)
			throws DatastoreException, NotFoundException, IOException;

	/**
	 * Delete a set of rows from a table.
	 * 
	 * @param models
	 */
	public RowReferenceSet deleteRows(UserInfo user, String tableId, List<ColumnModel> models, RowSelection rowsToDelete)
			throws DatastoreException, NotFoundException, IOException;

	/**
	 * Delete all rows from a table.
	 * 
	 * @param models
	 */
	public void deleteAllRows(String id);

	/**
	 * Append all rows from the provided iterator into the a table. This method
	 * will batch rows into optimum sized RowSets.
	 * 
	 * Note: This method will only keep one batch of rows in memory at a time so
	 * it should be suitable for appending very large change sets to a table.
	 * 
	 * @param user The user appending the rows
	 * @param tableId The ID of the table entity to append the rows too.
	 * @param models The schema of the rows being appended.
	 * @param rowStream The stream of rows to append to the table.
	 * @param etag
	 *            The last etag read before apply an update. An etag must be
	 *            provide if any rows are being updated.
	 * @param results
	 *            This parameter is optional. When provide, it will be populated
	 *            with a RowReference for each row appended to the table. This
	 *            parameter should be null for large change sets to minimize
	 *            memory usage.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	String appendRowsAsStream(UserInfo user, String tableId,
			List<ColumnModel> models, Iterator<Row> rowStream, String etag,
			RowReferenceSet results) throws DatastoreException,
			NotFoundException, IOException;

	/**
	 * Get the current ColumnModel list for a table.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public List<ColumnModel> getColumnModelsForTable(String tableId)
			throws DatastoreException, NotFoundException;

	/**
	 * List the changes that have been applied to a table.
	 * 
	 * @param tableId
	 * @return
	 */
	public List<TableRowChange> listRowSetsKeysForTable(String tableId);

	/**
	 * Get a specific RowSet.
	 * 
	 * @param tableId
	 * @param rowVersion
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public RowSet getRowSet(String tableId, Long rowVersion, Set<Long> rowsToGet)
			throws IOException, NotFoundException;

	/**
	 * Get all the rows and their current versions
	 * 
	 * @param tableId
	 * @param minVersion
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public Map<Long, Long> getCurrentRowVersions(String tableId, Long minVersion, long rowIdOffset, long limit) throws IOException,
			NotFoundException;

	/**
	 * Get the last table row change
	 * 
	 * @param tableId
	 * @return
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public TableRowChange getLastTableRowChange(String tableId) throws IOException, NotFoundException;

	/**
	 * Get the highest possible row id in this table
	 * 
	 * @param tableId
	 * @return the highest possible row id
	 * @throws IOException
	 * @throws NotFoundException
	 */
	public long getMaxRowId(String tableId) throws IOException, NotFoundException;;

	/**
	 * Get the values for a specific row reference and column
	 * 
	 * @param userInfo
	 * @param refSet
	 * @param model
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public String getCellValue(UserInfo userInfo, String tableId, RowReference rowRef, ColumnModel model) throws IOException,
			NotFoundException;

	/**
	 * Get the values for a specific row reference set and columns
	 * 
	 * @param userInfo
	 * @param refSet
	 * @param model
	 * @return
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public RowSet getCellValues(UserInfo userInfo, String tableId, RowReferenceSet rowRefs, List<ColumnModel> models)
			throws IOException, NotFoundException;

	/**
	 * <p>
	 * Attempt to acquire an exclusive lock on a table. If the lock is acquired,
	 * the passed Callable will be run while holding lock. The lock will
	 * automatically be release when the caller returns.
	 * </p>
	 * There are several possible conditions that can occur.
	 * <ul>
	 * <li>An exclusive lock has already been issued to another caller. A
	 * LockUnavilableException will be thrown for this case.</li>
	 * <li>One or more non-exclusive locks have been issued for this table. When
	 * this occurs, a reserve will placed that will block all new non-exclusive
	 * and exclusive locks. A wait loop will be started to wait for all
	 * outstanding non-exclusive locks to be release. Once all non-exclusive
	 * locks are release, the exclusive lock will be issued and the passed
	 * Caller will be run.</li>
	 * <li>Another caller has reserved the exclusive lock and is waiting for the
	 * exclusive lock. A LockUnavilableException will be thrown for this case.</li>
	 * <li>There are no outstanding non-exclusive locks, no executive lock
	 * reserver, and no exclusive lock. For this case, the reserver and
	 * exclusive lock will be acquired and the Callable will be run.</li>
	 * </ul>
	 * 
	 * @param tableId
	 * @param runner
	 * @throws LockUnavilableException
	 *             Thrown when an exclusive lock cannot be acquired.
	 * 
	 * @return
	 * @throws Exception
	 * @throws InterruptedException
	 */
	public <T> T tryRunWithTableExclusiveLock(String tableId, long timeoutMS,
			Callable<T> runner) throws LockUnavilableException,
			InterruptedException, Exception;

	/**
	 * <p>
	 * Attempt to acquire a non-exclusive lock on a table. If the lock is
	 * acquired, the passed Callable will be run while holding lock. The lock
	 * will automatically be release when the caller returns.
	 * </p>
	 * There are several possible conditions that can occur.
	 * <ul>
	 * <li>An exclusive lock has already been issued to another caller. A
	 * LockUnavilableException will be thrown for this case.</li>
	 * <li>One or more non-exclusive locks have been issued for this table. When
	 * this occurs another new non-exclusive lock will be acquired and the
	 * passed Callable will be run. There is no limit to the number of
	 * non-exclusive locks that can be issued for a single table.</li>
	 * <li>Another caller has reserved the exclusive lock and is waiting for the
	 * exclusive lock. A LockUnavilableException will be thrown for this case.</li>
	 * <li>There are no outstanding locks on this table at all. A new
	 * non-exclusive lock will be issue and the passed Callable will be run.</li>
	 * </ul>
	 * 
	 * @param tableId
	 * @param runner
	 * @return
	 * @throws LockUnavilableException
	 * @throws Exception
	 */
	public <T> T tryRunWithTableNonexclusiveLock(String tableId,
			long timeoutMS, Callable<T> runner) throws LockUnavilableException,
			Exception;

	/**
	 * Get the table status. If the status does not exist (table never indexed after a migration), then a status is
	 * created, a change message is generated and the new processing status is returned
	 * 
	 * @param tableId
	 * @return the status
	 * @throws NotFoundException if the table does not exist
	 */
	public TableStatus getTableStatusOrCreateIfNotExists(String tableId) throws NotFoundException;

	/**
	 * Attempt to set the table status to AVIALABLE. The state will be changed
	 * will be applied as long as the passed resetToken matches the current
	 * restToken indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken, String tableChangeEtag)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to set the table status to FAILED. The state will be changed will
	 * be applied as long as the passed resetToken matches the current restToken
	 * indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 * @throws NotFoundException
	 */
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, String errorMessage, String errorDetails)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to update the progress of a table. Will fail if the passed
	 * rest-token does not match the current reset-token indicating the table
	 * change while it was being processed.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @param progressMessage
	 * @param currentProgress
	 * @param totalProgress
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished.
	 * @throws NotFoundException
	 */
	public void attemptToUpdateTableProgress(String tableId, String resetToken,
			String progressMessage, Long currentProgress, Long totalProgress)
			throws ConflictingUpdateException, NotFoundException;

	/**
	 * 
	 * @param user
	 * @param sql
	 * @param isConsistent
	 * @param countOnly
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws TableUnavilableException
	 */
	public RowSet query(UserInfo user, String sql, boolean isConsistent,
			boolean countOnly) throws DatastoreException, NotFoundException,
			TableUnavilableException;
	
	/**
	 * Execute a table query.
	 * @param user
	 * @param query
	 * @param isConsistent
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavilableException
	 */
	public RowSet query(UserInfo user, SqlQuery query, boolean isConsistent) throws DatastoreException, NotFoundException,
			TableUnavilableException;

	/**
	 * Create an query object for the given SQL.
	 * 
	 * @param sql
	 * @param countOnly
	 * @return
	 */
	SqlQuery createQuery(String sql, boolean countOnly);

	/**
	 * Run a query while holding a non-exclusive lock (read lock) on the table. This method will load all resulting rows
	 * into memory at on time an should only be used if there is a limit to the number of rows read.
	 * 
	 * @param query
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 */
	RowSet runConsistentQuery(SqlQuery query) throws TableUnavilableException, NotFoundException;

	/**
	 * Run a query while holding a non-exclusive lock (read lock) on the table. This method will stream over the rows
	 * and will not keep the row data in memory. This method can be used to stream over results sets that are larger
	 * than the available system memory, as long as the caller does not hold the resulting rows in memory.
	 * 
	 * @param query
	 * @param handler
	 * @return The etag of the last change set applied to the table index.
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 */
	String runConsistentQueryAsStream(SqlQuery query, RowAndHeaderHandler handler) throws TableUnavilableException, NotFoundException;

	/**
	 * Run the provided SQL query string and stream the results to the passed CSVWriter. This method will stream over
	 * the rows and will not keep the row data in memory. This method can be used to stream over results sets that are
	 * larger than the available system memory, as long as the caller does not hold the resulting rows in memory.
	 * 
	 * @param sql
	 * @param writer
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 */
	AsynchDownloadFromTableResponseBody runConsistentQueryAsStream(String sql, CSVWriterStream writer, boolean includeRowIdAndVersion)
			throws TableUnavilableException, NotFoundException;

	/**
	 * Update the current version cache if enabled
	 */
	void updateLatestVersionCache(String tableId, ProgressCallback<Long> progressCallback) throws IOException;

	void removeLatestVersionCache(String tableId) throws IOException;

	/**
	 * Get the maximum number of rows allowed for a single page (get, put, or query) for the given columns.
	 * @param models
	 * @return
	 */
	public Long getMaxRowsPerPage(List<ColumnModel> models);

	/**
	 * Get the columns Models for a list of headers.  Only headers that are column models ID will have a column model in the result.
	 * None column model id headers will be ignored.
	 * 
	 * @param headers
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public List<ColumnModel> getColumnsForHeaders(List<String> headers) throws DatastoreException, NotFoundException;
}
