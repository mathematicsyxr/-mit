package simpledb.execution;

import simpledb.storage.Field;
import simpledb.storage.Tuple;

import java.io.Serializable;

/**
 * Predicate compares tuples to a specified Field value.
 * ���˲������൱�ڲ�ѯ������where�Ӿ�
 */
public class Predicate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Constants used for return codes in Field.compare
     */
    public enum Op implements Serializable {//enumö����
        EQUALS, GREATER_THAN, LESS_THAN, LESS_THAN_OR_EQ, GREATER_THAN_OR_EQ, LIKE, NOT_EQUALS;
        //���ڣ����ڣ�С�ڣ�С�ڵ��ڣ����ڵ��ڣ�������

        /**
         * Interface to access operations by integer value for command-line
         * convenience.
         *
         * @param i a valid integer Op index(��Ч������������)
         */
        public static Op getOp(int i) {
            return values()[i];
        }

        public String toString() {//���ݱȽϷ�����Ӧ�ķ��ű�ʾ
            if (this == EQUALS)
                return "=";
            if (this == GREATER_THAN)
                return ">";
            if (this == LESS_THAN)
                return "<";
            if (this == LESS_THAN_OR_EQ)
                return "<=";
            if (this == GREATER_THAN_OR_EQ)
                return ">=";
            if (this == LIKE)
                return "LIKE";
            if (this == NOT_EQUALS)
                return "<>";
            throw new IllegalStateException("impossible to reach here");
        }

    }

    private  int fields;
    private  Op ops;
    private Field operands;
    /**
     * Constructor.
     *
     * @param field   field number of passed in tuples to compare against.(tuple����һ�����ݡ�)
     * @param op      operation to use for comparison�����ӣ�
     * @param operand field value to compare passed in tuples to(Ҫ�Ƚϵ��ֶ�)
     */
    public Predicate(int field, Op op, Field operand) {
        // TODO: some code goes here
        this.fields=field;
        this.ops=op;
        this.operands=operand;
    }

    /**
     * @return the field number��tuple����һ�����ݣ�
     */
    public int getField() {
        // TODO: some code goes here
        return fields;
    }

    /**
     * @return the operator���������ӣ�
     */
    public Op getOp() {
        // TODO: some code goes here
        return ops;
    }

    /**
     * @return the operand��Ҫ�Ƚϵ��ֶΣ�����where id>=100 ,����100��Ϊoperand
     */
    public Field getOperand() {
        // TODO: some code goes here
        return operands;
    }

    /**
     * Compares the field number of t specified in the constructor to the
     * operand field specified in the constructor using the operator specific in
     * the constructor. The comparison can be made through Field's compare
     * method.
     *
     * @param t The tuple to compare against//����Ƚϵ�Ԫ��
     * @return true if the comparison is true, false otherwise.
     */
    public boolean filter(Tuple t) {//���Ȼ�ȡԪ����Ӧ�е�ֵ�����ýӿں���Filed��compare�������й��˷���
        // TODO: some code goes here
        Field get_operand=t.getField(fields);
        return get_operand.compare(ops,operands);
    }

    /**
     * Returns something useful, like "f = field_id op = op_string operand =
     * operand_string"
     */
    public String toString() {
        // TODO: some code goes here
        return String.format("f=%d op=%s operand=%s",fields,ops,operands);
    }
}
