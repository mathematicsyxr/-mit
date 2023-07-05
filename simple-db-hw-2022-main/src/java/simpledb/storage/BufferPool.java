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
    private LockManager lockManager;

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
     *       1.������
     *       2.�ͷ���
     *       3.�ж�ָ�������Ƿ����ĳһpage�ϵ���
     */
    private class LockManager{
        ConcurrentHashMap<PageId,ConcurrentHashMap<TransactionId,PageLock>> lockMap;
        public LockManager(){
            this.lockMap=new ConcurrentHashMap<>();
        }

        /**
         * Return true if the specified transaction has a lock on the specified page
         * @param tid ��Ҫ�����жϵ�����
         * @param p ��Ҫ�����жϵ�page
         * @return
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

        /**
         * �ͷ�ָ������ָ��ҳ�ϵ���
         * @param tid ��Ҫ�����ͷ�������������
         * @param pid ��Ҫ�����ͷ�����ҳ
         */
        public synchronized void releaselock(TransactionId tid,PageId pid){
            if(holdsLock(tid,pid)){
                ConcurrentHashMap<TransactionId,PageLock> pagelocks;
                pagelocks=lockMap.get(pid);
                pagelocks.remove(tid);
                if(pagelocks.size()==0)
                {
                    lockMap.remove(pid);
                }
            }
            this.notify();
        }

        /**
         * ĳ������ĳҳ�������
         * @param tid �������������
         * @param pid ���������ҳ
         * @param lockType �������������
         * @return
         * ����ԭ��
         * 1.��һ��������Զ�һ������֮ǰ���������ڶ�������һ����������
         * 2.���������д����֮ǰ��������Զ�����л�������
         * 3.���������Զ�һ��������һ����������
         * 4.ֻ��һ��������Զ�һ������ӵ�л�������
         * 5.������� t ��Ψһ���ж����ϵĹ�������������ô���� t ���Խ���Զ���  ��������Ϊ������
         * ����ʵ�֣�1.���жϸ�ҳ�Ƿ����������û����ֱ�ӽ��м�����������������������Ϣ����map��
         *         2.��ҳ����ȷ�������������������һ��������tid��������һ��������tid��û��
         *         2.1tid��������   �������S����ֱ������
         *                        �������X�������t��Ψһ���й������������������������
         *                        ���򣬲��ܽ������������ܳ����������ȴ�/�׳��쳣
         *          2.2����t�ϵ������������S�������ֻ��S����ֱ�ӻ�ȡ���������X�����ȴ�/�׳��쳣
         *                        �������X�����ȴ����׳��쳣
         */
        //0ΪS����1ΪX��
        // ConcurrentHashMap<PageId,ConcurrentHashMap<TransactionId,PageLock>> lockMap;
        public synchronized boolean getLocks(TransactionId tid,PageId pid,int lockType) throws InterruptedException {
            if (lockMap.get(pid)==null)
            {
                return putLock(tid,pid,lockType);
            }
            //��ȡҳ���ϵ���
            ConcurrentHashMap<TransactionId,PageLock> page_lock=lockMap.get(pid);
            //�ж��Ƿ�Ϊ��������tid�ϵ���
            //û������tid�ϵ���
            if(page_lock.get(tid)==null){
                if(lockType==1)//����X��
                {
                    wait(100);
                    System.out.println("Can't get the exclusive lock because other transactionId has locks!");
                    return false;
                } else if (lockType == 0) {//����S��
                    if(page_lock.size()>1)//ҳ���ϵ�������������1˵��ֻ��S��
                    {
                        return putLock(tid,pid,lockType);
                    } else if (page_lock.size()==1) {
                        Collection<PageLock> p=page_lock.values();
                        for (PageLock value : p)
                        {
                            if(value.getLocktype()==0)//����е�һ����ΪS��
                            {
                                return putLock(tid,pid,lockType);
                            }else{
                                wait(100);
                                return false;
                            }
                        }

                    }
                }
            }else{//����tid������
                if(lockType==0){//�����ΪS��
                    return true;
                }else{
                    if(page_lock.get(tid).getLocktype()==1){
                        return  true;
                    }else {
                        if(page_lock.size()>1)
                        {
                            wait(100);
                            return false;
                        }else{
                            page_lock.remove(tid);
                            return putLock(tid,pid,lockType);
                        }
                    }
                }
            }
            return false;
        }
        public boolean putLock(TransactionId tid, PageId pid,int lockType){
            PageLock pagelocks=new PageLock(tid,lockType);
            ConcurrentHashMap<TransactionId,PageLock> p=lockMap.get(pid);
            if(p==null)
            {
                p=new ConcurrentHashMap<>();
                lockMap.put(pid,p);
            }
            p.put(tid,pagelocks);
            lockMap.put(pid,p);
            return true;
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
        this.lockManager=new LockManager();
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
        int lockType;
        if (perm == Permissions.READ_ONLY){
            lockType = 0;
        } else {
            lockType = 1;
        }
        long st = System.currentTimeMillis();
        boolean isacquired = false;
        while(!isacquired){
            try {
                isacquired =lockManager.getLocks(tid,pid,lockType);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long now = System.currentTimeMillis();
            if(now - st > 300){
                throw new TransactionAbortedException();
            }
        }
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
        lockManager.releaselock(tid,pid);
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



