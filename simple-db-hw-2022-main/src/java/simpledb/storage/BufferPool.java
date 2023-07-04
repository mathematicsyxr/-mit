package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /**
     * Bytes per page, including header.
     */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;
    private final Page[] buffer;
    private int numPages;
    private final Map<PageId,Page> page_store;

    /**
     * ҳ�����
     */
    private class PageLock{
        private TransactionId tid;
        private int locktype;//0Ϊ��������1Ϊ������

        public PageLock(TransactionId tid, int locktype) {
            this.tid = tid;
            this.locktype = locktype;
        }

        public TransactionId getTid() {
            return tid;
        }

        public void setTid(TransactionId tid) {
            this.tid = tid;
        }

        public int getLocktype() {
            return locktype;
        }

        public void setLocktype(int locktype) {
            this.locktype = locktype;
        }
    }
    /**
     * ���Ĺ�������������ͷ���
     * 1.������
     * 2.�ͷ���
     * 3.�ж�ָ�������Ƿ����ĳһpage�ϵ���
     */
    private class LockManager{
        ConcurrentHashMap<PageId,ConcurrentHashMap<TransactionId,PageLock>> lockMap;
        public LockManager(){
            this.lockMap=new ConcurrentHashMap<>();
        }
        /**
         * Return true if the specified transaction has a lock on the specified page
         */
        public boolean holdsLock(TransactionId tid, PageId p) {//�ж�ָ�������Ƿ����ĳһpage�ϵ���
            // TODO: some code goes here
            // not necessary for lab1|lab2
            ConcurrentHashMap<TransactionId,PageLock> pagelocks;
            pagelocks=lockMap.get(p);
            if(pagelocks==null)
            {
                return false;
            }
           for(TransactionId t:pagelocks.keySet())
           {
               if(t==tid)
               {
                   return true;
               }
           }
           return false;
        }


    }
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        this.numPages = numPages;
        buffer = new Page[numPages];
        this.page_store=new HashMap<>();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)//��̫���ø����������ID��
    // TransactionId ����������������ֵΪmull��
            throws TransactionAbortedException, DbException {
        /*int idx = -1;
        for (int i = 0; i < buffer.length; ++i) {
            if (null == buffer[i]) {//��������û����buffer[i]Ϊ�գ���¼�����λ��
                idx = i;
            } else if (pid.equals(buffer[i].getId())) {
                return buffer[i];//���������ֱ�ӷ���ҳ
            }
        }
        //ȥdisk�ҽ�buffer[i]����
        return buffer[idx] = Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);*/
        if(!page_store.containsKey(pid))
        {
            if(page_store.size()>numPages)
            {
                evictPage();
            }
            Page p=Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);
            page_store.put(pid,p);
        }
        return page_store.get(pid);
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void unsafeReleasePage(TransactionId tid, PageId pid) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
    }


    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
    }
    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        List<Page> p=Database.getCatalog().getDatabaseFile(tableId).insertTuple(tid,t);//�����б�������޸ĵ�ҳ��
        boolean in=false;
        for (Page page : p) {
            page.markDirty(true, tid);
            if(page_store.size()<numPages)//buffer���п���λ��
            {
                in=true;
            }
            for(int i=0;i<numPages;i++)
            {
                if(buffer[i]==page.getId())//��ҳ�Ѿ����浽buffer��
                {
                    in=true;
                }
            }
            if(in)//���Բ���
            {
                page_store.put(page.getId(),page);
            }
            else//��ʱ˵��buffer�Ѿ�����
            {
                evictPage();
                page_store.put(page.getId(),page);
            }
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        List<Page> p=Database.getCatalog().getDatabaseFile
                (t.getRecordId().getPageId().getTableId()).deleteTuple(tid,t);//�����б�������޸ĵ�ҳ��
        for (Page page : p) {
            page.markDirty(true, tid);
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        for(Page p:page_store.values()){
            flushPage(p.getId());
        }
    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     * <p>
     * Also used by B+ tree files to ensure that deleted pages
     * are removed from the cache so they can be reused safely
     */
    public synchronized void removePage(PageId pid) {
        // TODO: some code goes here
        // not necessary for lab1
        page_store.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        Page p=page_store.get(pid);
        Database.getCatalog().getDatabaseFile(p.getId().getTableId()).writePage(p);
        p.markDirty(false,null);
    }

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1|lab2
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        // TODO: some code goes here
        // not necessary for lab1
    }
}



