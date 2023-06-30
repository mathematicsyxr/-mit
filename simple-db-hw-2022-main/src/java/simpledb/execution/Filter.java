package simpledb.execution;

import simpledb.common.DbException;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.util.NoSuchElementException;

/**
 * Filter is an operator that implements a relational select.
 */
public class Filter extends Operator {//�̳�Operator

    private static final long serialVersionUID = 1L;
    private final Predicate p;//����ɸѡԪ���ν��
    private  OpIterator child;//����ɸѡԪ��ĵ���������
    private final TupleDesc tupleDesc;//����ɸѡ��Ԫ���Ԫ��Ϣ
    /**
     * Constructor accepts a predicate to apply and a child operator to read
     * tuples to filter from.
     *
     * @param p     The predicate to filter tuples with//����ɸѡԪ���ν��
     * @param child The child operator//����ɸѡ��Ԫ��
     */
    public Filter(Predicate p, OpIterator child) {
        // TODO: some code goes here
        this.p=p;
        this.child=child;
        this.tupleDesc=child.getTupleDesc();

    }

    public Predicate getPredicate() {//��ȡɸѡ������
        // TODO: some code goes here
        return p;
    }

    public TupleDesc getTupleDesc() {//��ȡԪ��Ԫ��Ϣ
        // TODO: some code goes here
        return tupleDesc;
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // TODO: some code goes here
        super.open();
        child.open();
    }

    public void close() {
        // TODO: some code goes here
        super.close();
        child.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        child.rewind();
    }

    /**
     * AbstractDbIterator.readNext implementation. Iterates over tuples from the
     * child operator, applying the predicate to them and returning those that
     * pass the predicate (i.e. for which the Predicate.filter() returns true.)
     *
     * @return The next tuple that passes the filter, or null if there are no
     *         more tuples
     * @see Predicate#filter
     */
    protected Tuple fetchNext() throws NoSuchElementException,
            TransactionAbortedException, DbException {//��ȡ��һ��Ԫ��
        // TODO: some code goes here
        while (child.hasNext())//�������е�Ԫ��
        {
            Tuple tuple=child.next();//��ȡ��һ��Ԫ��
            if(p.filter(tuple))//�ж��Ƿ����ɸѡ����
            {
                return tuple;
            }
        }
        return null;
    }

    @Override
    public OpIterator[] getChildren() {//���ص�����
        // TODO: some code goes here
        return new OpIterator[] {this.child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // TODO: some code goes here
        this.child=children[0];
    }

}
