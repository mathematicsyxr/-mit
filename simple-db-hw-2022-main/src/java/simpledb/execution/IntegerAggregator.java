package simpledb.execution;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.Field;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.util.*;

/**
 * �ۺ�
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private final int gbfield;
    private final Type gbfieldtype;
    private final int afield;
    private final Op what;
    private TupleDesc aggDesc;
    private final Map<Field,Integer> resultgroup;//MIN, MAX, SUM,COUNT
    private final Map<Field, List<Integer>> avggroup;//AVG

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or
     *                    NO_GROUPING if there is no grouping
     *                    ��Ԫ���з��������ֶεĴ� 0 ��ʼ�����������û�з��飬��Ϊ NO_GROUPING��
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null
     *                    if there is no grouping
     *                    �����������ֶε����ͣ����磬Type.INT_TYPE�������û�з��飬��Ϊ null��
     * @param afield      the 0-based index of the aggregate field in the tuple
     *                    ��Ԫ���оۺ��ֶεĴ� 0 ��ʼ��������
     * @param what        the aggregation operator���ۺ��������
     *                    MIN, MAX, SUM, AVG, COUNT,(SUM_COUNT,SC_AVG)lab7;
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // TODO: some code goes here
        this.gbfield=gbfield;
        this.gbfieldtype=gbfieldtype;
        this.afield=afield;
        this.what=what;
        this.resultgroup=new HashMap<>();
        this.avggroup=new HashMap<>();
        if (this.gbfield>=0) {
            // ��groupBy
            this.aggDesc = new TupleDesc(new Type[]{this.gbfieldtype,Type.INT_TYPE},
                    new String[]{"groupVal","aggregateVal"});
        } else {
            // ��groupBy
            this.aggDesc = new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{"aggregateVal"});
        }
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     *            �������ۺ��ֶκͷ��������ֶε�Ԫ�飩
     */
    /**
     * select MAX(Sage) FROM Student Where Sdept='CS';
     * select MIN(Sage) FROM Student where Sdept='CS';
     * select sum(money) as ������ from table GROUP BY xx
     * select COUNT(Grade) FROM SC;
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        //TODO:(SUM_COUNT,SC_AVG)lab7;
        // TODO: some code goes here
        IntField afield=(IntField) tup.getField(this.afield);//��ȡ�ۺ��ֶ�
        Field gbfield=this.gbfield==NO_GROUPING?null: tup.getField(this.gbfield);
        int value=afield.getValue();
        if(gbfield!=null&&gbfield.getType()!=this.gbfieldtype)
        {
            throw new IllegalArgumentException();
        }
        switch (this.what){
            case MAX://�ж�gbfield�Ƿ�ֵ������н��бȽϷ���ϴ�ģ����û��ֱ�ӷ���
                if(this.resultgroup.containsKey(gbfield))
                {
                    this.resultgroup.put(gbfield,Math.max(this.resultgroup.get(gbfield),value));
                }
                else
                {
                    this.resultgroup.put(gbfield,value);
                }
                break;
            case MIN://�ж�gbfield�Ƿ���ֵ������з����С�ģ����û��ֱ�ӷ���
                if(this.resultgroup.containsKey(gbfield))
                {
                    this.resultgroup.put(gbfield,Math.min(this.resultgroup.get(gbfield),value));
                }
                else
                {
                    this.resultgroup.put(gbfield,value);
                }
                break;
            case SUM://�ж�gbfield�Ƿ���ֵ������з�����ӷ��룬���û��ֱ�ӷ���
                if(this.resultgroup.containsKey(gbfield))
                {
                    this.resultgroup.put(gbfield,this.resultgroup.get(gbfield)+value);
                }
                else
                {
                    this.resultgroup.put(gbfield,value);
                }
                break;
            case COUNT://�ж�gbfield�Ƿ���ֵ������м�����1�����û��ֱ�ӷ���
                if(!this.resultgroup.containsKey(gbfield))
                {
                    this.resultgroup.put(gbfield,1);
                }
                else
                {
                    this.resultgroup.put(gbfield,this.resultgroup.get(gbfield)+1);
                }
                break;
            case AVG://����������ƽ��ֵ
                if(!this.avggroup.containsKey(gbfield))//gbfield������ֵ��ֱ�ӷ��뼴Ϊƽ��ֵ
                {
                    List<Integer> l=new ArrayList<>();
                    l.add(value);
                    this.avggroup.put(gbfield,l);
                }
                else//���������ºͻ��г��ȼ���ƽ��ֵ
                {
                    List<Integer> l = this.avggroup.get(gbfield);
                    l.add(value);
                    int sum=0;
                    for (Integer integer : l) {
                        sum += integer;
                    }
                    List<Integer> l1=new ArrayList<>();
                    l1.add(sum /l.size());
                    this.avggroup.put(gbfield,l1);
                }
                break;
            default:
                throw new IllegalArgumentException("Aggregate not supported!");
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public OpIterator iterator() {//���ص�����
        // TODO: some code goes here
        return new IntAggIterator();
    }

    private class IntAggIterator implements OpIterator {
        //����Map.Entry����һ�Լ�ֵ
        private Iterator<Map.Entry<Field, List<Integer>>> avgIt;//����AVG
        private  Iterator<Map.Entry<Field,Integer>> it;
        /*
        ��ÿ���������ж��Ƿ�ΪAVG
         */
        @Override
        public void open() throws DbException, TransactionAbortedException {
            if(what.equals(Op.AVG))
            {
                avgIt=avggroup.entrySet().iterator();
            }
            it=resultgroup.entrySet().iterator();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if(what.equals(Op.AVG))
            {
                return avgIt.hasNext();
            }
            return it.hasNext();
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            Tuple tp=new Tuple(aggDesc);
            if(what.equals(Op.AVG))
            {
                Map.Entry<Field, List<Integer>> avgOrSumCountEntry = this.avgIt.next();
                Field avgOrSumCountField = avgOrSumCountEntry.getKey();
                List<Integer> avgOrSumCountList = avgOrSumCountEntry.getValue();
                int value = this.sumList(avgOrSumCountList) / avgOrSumCountList.size();
                this.setFields(tp, value, avgOrSumCountField);
                return tp;
            }
            Map.Entry<Field,Integer> m=this.it.next();
            Field mfield=m.getKey();
            this.setFields(tp,m.getValue(),mfield);
            return tp;
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            this.close();
            this.open();
        }

        @Override
        public TupleDesc getTupleDesc() {
            return aggDesc;
        }

        @Override
        public void close() {
        }
        private int sumList(List<Integer> l) {
            int sum = 0;
            for (int i : l)
                sum += i;
            return sum;
        }
        void setFields(Tuple rtn, int value, Field f) {
            if (f == null) {
                rtn.setField(0, new IntField(value));
            } else {
                rtn.setField(0, f);
                rtn.setField(1, new IntField(value));
            }
        }
    }
}

