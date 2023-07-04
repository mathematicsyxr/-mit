package simpledb.optimizer;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.execution.Predicate;
import simpledb.execution.SeqScan;
import simpledb.storage.*;
import simpledb.transaction.Transaction;
import simpledb.transaction.TransactionAbortedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query.
 * TableStats ��ʾ�йػ����е�ͳ����Ϣ�����磬ֱ��ͼ����ѯ��
 * <p>
 * This class is not needed in implementing lab1 and lab2.
 */
public class TableStats {

    private static final ConcurrentMap<String, TableStats> statsMap = new ConcurrentHashMap<>();

    static final int IOCOSTPERPAGE = 1000;//ÿҳIO�Ŀ���

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }

    public static void setStatsMap(Map<String, TableStats> s) {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     * ֱ��ͼ�������������������⽫��ֵ���ӵ� 100 ���ϣ��������ǵĲ��Լ���ֱ��ͼ�������� 100 ����������
     */
    static final int NUM_HIST_BINS = 100;

    private int numtuples;//Ԫ�������
    private int numpages;//ҳ������
    private int numfield;//�ֶε�����
    private int iocostperpage;//ÿҳio�Ĵ���
    private int tableid;//���ڼ���ͳ����Ϣ�ı�
    private Map<Integer,IntHistogram> intHistogramMap;
    private Map<Integer,StringHistogram> stringHistogramMap;
    private Map<Integer,Integer> max;//����ֶ�ӳ��
    private Map<Integer,Integer> min;//��С�ֶ�ӳ��
    private DbFile dbFile;
    private HeapFile table;//��Ҫ��������ͳ�Ƶı�
    private TupleDesc td;
    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     * ����һ���µ� TableStats �������ڸ��ٱ��ÿһ�е�ͳ����Ϣ
     *
     * @param tableid       The table over which to compute statistics���ڼ���ͳ����Ϣ�ı�
     * @param ioCostPerPage The cost per page of IO. This doesn't differentiate between
     *                      sequential-scan IO and disk seeks.ÿҳ�Ĵ��ۣ��ⲻ������˳��ɨ�� IO �ʹ���Ѱ��
     */
    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // TODO: some code goes here
        this.tableid=tableid;
        this.iocostperpage=ioCostPerPage;
        this.intHistogramMap=new HashMap<>();
        this.stringHistogramMap=new HashMap<>();
        this.dbFile=Database.getCatalog().getDatabaseFile(tableid);
        this.table=(HeapFile) Database.getCatalog().getDatabaseFile(tableid);
        this.numtuples=0;
        this.numpages=table.numPages();
        this.numfield=table.getTupleDesc().numFields();
        this.max=new HashMap<>();
        this.min=new HashMap<>();
        this.td=dbFile.getTupleDesc();
        Transaction t=new Transaction();
        t.start();
        DbFileIterator iterator=table.iterator(t.getId());
        try{
            iterator.open();
            while (iterator.hasNext())
            {
                Tuple tp=iterator.next();
                this.numtuples++;
                for(int i=0;i<td.numFields();i++)
                {
                    //����int
                    if(td.getFieldType(i).equals(Type.INT_TYPE))
                    {
                        IntField intField= (IntField) tp.getField(i);
                        int value=intField.getValue();
                        //����int��min
                        if(min.get(i)==null||value<min.get(i))
                        {
                            min.put(i,value);
                        } else if (max.get(i)==null||value>max.get(i)) {
                            max.put(i,value);
                        }
                    }
                    else if(td.getFieldType(i).equals(Type.STRING_TYPE)){
                        StringField stringField=(StringField) tp.getField(i);
                        StringHistogram stringHistogram = new StringHistogram(NUM_HIST_BINS);
                        stringHistogram.addValue(stringField.getValue());
                        stringHistogramMap.put(i,stringHistogram);

                    }
                }
            }
            //���������Сֵ����ֱ��ͼ
            for (int i = 0; i < td.numFields(); i++) {
                if(min.get(i)!=null)
                {
                    this.intHistogramMap.put(i,new IntHistogram(NUM_HIST_BINS,min.get(i),max.get(i)));
                }

            }
            iterator.rewind();
            //���ֵ
            while (iterator.hasNext())
            {
                Tuple tp=iterator.next();
                for(int i=0;i< td.numFields();i++)
                {
                    if(td.getFieldType(i).equals(Type.INT_TYPE))
                    {
                        IntField f=(IntField)tp.getField(i);
                        IntHistogram in=intHistogramMap.get(i);
                        in.addValue(f.getValue());
                        this.intHistogramMap.put(i,in);
                    }
                }
            }


        }  catch (DbException | TransactionAbortedException e) {
            throw new RuntimeException(e);
        }finally {
            iterator.close();
            try {
                t.commit();
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * <p>
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     *
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        // TODO: some code goes here
        return table.numPages()*iocostperpage*2;
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     *
     * @param selectivityFactor The selectivity of any predicates over the table�κ�ν�ʶԱ��ѡ����
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        // TODO: some code goes here
        return (int)(numtuples*selectivityFactor);
    }

    /**
     * The average selectivity of the field under op.
     *
     * @param field the index of the field
     * @param op    the operator in the predicate
     *              The semantic of the method is that, given the table, and then given a
     *              tuple, of which we do not know the value of the field, return the
     *              expected selectivity. You may estimate this value from the histograms.
     *              ν���е�������÷����������ǣ�������Ȼ�����һ��Ԫ�飬���ǲ�֪���ֶε�ֵ��
     *              ����Ԥ�ڵ�ѡ���ԡ������Դ�ֱ��ͼ�й��ƴ�ֵ��
     */
    //�ж����ͷֱ����int��string��ֱ��ͼ
    public double avgSelectivity(int field, Predicate.Op op) {
        // TODO: some code goes here
        if(td.getFieldType(field).equals(Type.INT_TYPE))
        {
            return intHistogramMap.get(field).avgSelectivity();
        }
        else if (td.getFieldType(field).equals(Type.STRING_TYPE))
        {
            return stringHistogramMap.get(field).avgSelectivity();
        }
        return 1.0;
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     *
     * @param field    The field over which the predicate rangesν�ʷ�Χ���ֶ�
     * @param op       The logical operation in the predicateν���е��߼�����
     * @param constant The value against which the field is compared ���ֶν��бȽϵ�ֵ
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate���Ƶ�ѡ���ԣ�����ν�ʵ�Ԫ�������
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        // TODO: some code goes here
        if (td.getFieldType(field).equals(Type.INT_TYPE)) {
            IntField intField = (IntField) constant;
            return intHistogramMap.get(field).estimateSelectivity(op,intField.getValue());
        } else if(td.getFieldType(field).equals(Type.STRING_TYPE)){
            StringField stringField = (StringField) constant;
            return stringHistogramMap.get(field).estimateSelectivity(op,stringField.getValue());
        }
        return -1.00;
    }

    /**
     * return the total number of tuples in this table
     */
    public int totalTuples() {
        // TODO: some code goes here
        return numtuples;
    }

}
