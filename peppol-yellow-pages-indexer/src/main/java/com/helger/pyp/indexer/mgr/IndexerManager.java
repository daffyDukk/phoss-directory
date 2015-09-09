package com.helger.pyp.indexer.mgr;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.ToStringGenerator;
import com.helger.datetime.PDTFactory;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.photon.core.app.CApplication;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.IPYPBusinessInformationProvider;
import com.helger.pyp.indexer.domain.EIndexerWorkItemType;
import com.helger.pyp.indexer.domain.IndexerWorkItem;
import com.helger.pyp.indexer.domain.ReIndexWorkItem;
import com.helger.pyp.indexer.job.ReIndexJob;
import com.helger.pyp.storage.PYPStorageManager;
import com.helger.schedule.quartz.GlobalQuartzScheduler;

/**
 * The global indexer manager that takes an item for queuing and maintains the
 * uniqueness of the items to queue.
 *
 * @author Philip Helger
 */
public final class IndexerManager implements Closeable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (IndexerManager.class);
  private static final String ELEMENT_ROOT = "root";
  private static final String ELEMENT_ITEM = "item";

  private final ReadWriteLock m_aRWLock = new ReentrantReadWriteLock ();
  private final PYPStorageManager m_aStorageMgr;
  private final File m_aIndexerWorkItemFile;
  private final ReIndexWorkItemList m_aReIndexList;
  private final IndexerWorkItemQueue m_aIndexerWorkQueue = new IndexerWorkItemQueue (this::_asyncFetchParticipantData);
  private final TriggerKey m_aTriggerKey;
  @GuardedBy ("m_aRWLock")
  private final Set <IndexerWorkItem> m_aUniqueItems = new HashSet <> ();
  @GuardedBy ("m_aRWLock")
  private IPYPBusinessInformationProvider m_aBIProvider = new SMPBusinessInformationProvider ();

  // Status vars
  private final GlobalQuartzScheduler m_aScheduler;

  public IndexerManager (@Nonnull final PYPStorageManager aStorageMgr) throws DAOException
  {
    m_aStorageMgr = ValueEnforcer.notNull (aStorageMgr, "StorageMgr");
    m_aReIndexList = new ReIndexWorkItemList ();

    // Remember the file because upon shutdown WebFileIO may already be
    // discarded
    m_aIndexerWorkItemFile = WebFileIO.getDataIO ().getFile ("indexer-work-items.xml");

    // Schedule re-index job
    m_aTriggerKey = ReIndexJob.schedule (SimpleScheduleBuilder.repeatMinutelyForever (1), CApplication.APP_ID_SECURE);

    // remember here
    m_aScheduler = GlobalQuartzScheduler.getInstance ();
  }

  @Nonnull
  public IndexerManager readAndQueueInitialData ()
  {
    // Read the file - may not be existing
    final IMicroDocument aDoc = MicroReader.readMicroXML (m_aIndexerWorkItemFile);
    if (aDoc != null)
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Reading persisted indexer work items from " + m_aIndexerWorkItemFile);
      for (final IMicroElement eItem : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      {
        final IndexerWorkItem aWorkItem = MicroTypeConverter.convertToNative (eItem, IndexerWorkItem.class);
        _queueWorkItem (aWorkItem);
      }

      // Delete the files to ensure it is not read again next startup time
      WebFileIO.getFileOpMgr ().deleteFile (m_aIndexerWorkItemFile);
    }
    return this;
  }

  private void _writeWorkItems (@Nonnull final List <IndexerWorkItem> aItems)
  {
    if (!aItems.isEmpty ())
    {
      s_aLogger.info ("Persisting " + aItems.size () + " indexer work items");
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
      for (final IndexerWorkItem aItem : aItems)
        eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aItem, ELEMENT_ITEM));
      if (MicroWriter.writeToFile (aDoc, m_aIndexerWorkItemFile).isFailure ())
        throw new IllegalStateException ("Failed to write IndexerWorkItems to " + m_aIndexerWorkItemFile);
    }
  }

  public void close () throws IOException
  {
    // Get all remaining objects and save them for late reuse
    final List <IndexerWorkItem> aRemainingWorkItems = m_aIndexerWorkQueue.stop ();
    _writeWorkItems (aRemainingWorkItems);

    // Unschedule the job to avoid problems on shutdown. Use the saved instance
    // because GlobalQuartzScheduler.getInstance() would fail because the global
    // scope is already in destruction.
    m_aScheduler.unscheduleJob (m_aTriggerKey);

    // Close Lucene index etc.
    m_aStorageMgr.close ();
  }

  @Nonnull
  public IPYPBusinessInformationProvider getBusinessInformationProvider ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aBIProvider;
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnull
  public IndexerManager setBusinessInformationProvider (@Nonnull final IPYPBusinessInformationProvider aBIProvider)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aBIProvider = ValueEnforcer.notNull (aBIProvider, "BIProvider");
      return this;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  private EChange _queueWorkItem (@Nonnull final IndexerWorkItem aWorkItem)
  {
    ValueEnforcer.notNull (aWorkItem, "WorkItem");

    // Check for duplicate
    m_aRWLock.writeLock ().lock ();
    try
    {
      if (!m_aUniqueItems.add (aWorkItem))
      {
        s_aLogger.info ("Ignoring work item " + aWorkItem.getLogText () + " because it is already in the queue!");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    // Queue it
    m_aIndexerWorkQueue.queueObject (aWorkItem);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Queued work item " + aWorkItem.getLogText ());

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange queueWorkItem (@Nonnull final IParticipantIdentifier aParticipantID,
                                @Nonnull final EIndexerWorkItemType eType,
                                @Nonnull @Nonempty final String sOwnerID)
  {
    // Build item
    final IndexerWorkItem aWorkItem = new IndexerWorkItem (aParticipantID, eType, sOwnerID);
    // And queue it
    return _queueWorkItem (aWorkItem);
  }

  private void _removeFromOverallList (@Nonnull final IndexerWorkItem aItem)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aUniqueItems.remove (aItem);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  private ESuccess _onCreateOrUpdate (@Nonnull final IndexerWorkItem aItem) throws IOException
  {
    final IPeppolParticipantIdentifier aParticipantID = aItem.getParticipantID ();

    // Get BI from participant
    final BusinessInformationType aBI = getBusinessInformationProvider ().getBusinessInformation (aParticipantID);
    if (aBI == null)
    {
      // No/invalid extension present - no need to try again
      return ESuccess.FAILURE;
    }

    // Got data - put in storage
    return m_aStorageMgr.createOrUpdateEntry (aParticipantID, aBI, aItem.getOwnerID ());
  }

  @Nonnull
  private ESuccess _onDelete (@Nonnull final IndexerWorkItem aItem) throws IOException
  {
    final IPeppolParticipantIdentifier aParticipantID = aItem.getParticipantID ();

    return m_aStorageMgr.deleteEntry (aParticipantID);
  }

  @Nonnull
  private ESuccess _fetchParticipantData0 (@Nonnull final IndexerWorkItem aItem)
  {
    s_aLogger.info ("On " + aItem.getLogText ());

    try
    {
      ESuccess eSuccess;
      switch (aItem.getType ())
      {
        case CREATE_UPDATE:
          eSuccess = _onCreateOrUpdate (aItem);
          break;
        case DELETE:
          eSuccess = _onDelete (aItem);
          break;
        default:
          throw new IllegalStateException ("Unsupported item type: " + aItem);
      }

      if (eSuccess.isSuccess ())
      {
        // Item handled - remove from overall list
        _removeFromOverallList (aItem);
        return ESuccess.SUCCESS;
      }
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error in storage handling", ex);
      // Fall through
    }

    return ESuccess.FAILURE;
  }

  /**
   * This is the performer for the direct data fetching.
   *
   * @param aItem
   *        The item to be fetched. Never <code>null</code>.
   * @return {@link ESuccess}.
   */
  @Nonnull
  private ESuccess _asyncFetchParticipantData (@Nonnull final IndexerWorkItem aItem)
  {
    final ESuccess eSuccess = _fetchParticipantData0 (aItem);

    if (eSuccess.isFailure ())
    {
      s_aLogger.warn ("Error fetching " + aItem.getLogText ());
      // Failed to fetch participant data - add to re-index queue and leave in
      // the overall list
      m_aReIndexList.addItem (new ReIndexWorkItem (aItem));
    }
    return eSuccess;
  }

  /**
   * Expire all re-index entries that are in the list for a too long time.
   */
  public void expireOldEntries ()
  {
    // Expire old entries
    final List <ReIndexWorkItem> aExpiredItems = m_aReIndexList.getAndRemoveAllExpiredEntries ();
    if (!aExpiredItems.isEmpty ())
    {
      s_aLogger.info ("Expired " + aExpiredItems.size () + " re-index work items");

      m_aRWLock.writeLock ().lock ();
      try
      {
        // remove them from the overall list
        aExpiredItems.stream ().forEach (aItem -> m_aUniqueItems.remove (aItem.getWorkItem ()));
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }
    }
  }

  public void reIndexParticipantData ()
  {
    // Get and remove all items to re-index "now"
    final List <ReIndexWorkItem> aReIndexNowItems = m_aReIndexList.getAndRemoveAllItemsForReIndex (PDTFactory.getCurrentLocalDateTime ());

    for (final ReIndexWorkItem aReIndexItem : aReIndexNowItems)
    {
      s_aLogger.info ("Try to perform " + aReIndexItem.getLogText ());
      if (_fetchParticipantData0 (aReIndexItem.getWorkItem ()).isFailure ())
      {
        // Still no success. Add again to the retry list
        m_aReIndexList.incRetryCountAndAddItem (aReIndexItem);
      }
    }
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("UniqueItems", m_aUniqueItems)
                            .append ("ReIndexList", m_aReIndexList)
                            .append ("IndexerWorkQueue", m_aIndexerWorkQueue)
                            .append ("TriggerKey", m_aTriggerKey)
                            .append ("BIProvider", m_aBIProvider)
                            .toString ();
  }
}
