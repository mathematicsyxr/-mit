package simpledb.storage;

import simpledb.common.Type;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * ���鱣��������
     */
    private final TDItem[] tdItems ;
    /**
     * A help class to facilitate organizing the information of each field
     */
    public static class TDItem implements Serializable {//���л�

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         */
        public final Type fieldType;//�ֶ�����

        /**
         * The name of the field
         */
        public final String fieldName;//�ֶ�����

        public TDItem(Type t, String n) {//�������ֺ�����
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return An iterator which iterates over all the field TDItems
     *         that are included in this TupleDesc
     */
    public Iterator<TDItem> iterator() {//������������������
        // TODO: some code goes here
        return Arrays.asList(tdItems).iterator();
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr  array specifying the number of and types of fields in this
     *                TupleDesc. It must contain at least one entry.
     * @param fieldAr array specifying the names of the fields. Note that names may
     *                be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr)   {//���Կ��ǲ����б���������������
        // TODO: some code goes here
        tdItems=new TDItem[typeAr.length];
        for(int i=0;i<typeAr.length;i++)
        {
            tdItems[i]=new TDItem(typeAr[i],fieldAr[i]);
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param typeAr array specifying the number of and types of fields in this
     *               TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // TODO: some code goes here
        tdItems=new TDItem[typeAr.length];
        for(int i=0;i< typeAr.length;i++)
        {
            tdItems[i]=new TDItem(typeAr[i],"");//������δ֪
        }
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {//����������������
        // TODO: some code goes here
        return tdItems.length;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {//���ص�i��������
        // TODO: some code goes here
        if(i<0||i> tdItems.length)
        {
            throw new NoSuchElementException("Invalid name index.");
        }
        return tdItems[i].fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i The index of the field to get the type of. It must be a valid
     *          index.
     * @return the type of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {//���ص�i����������
        // TODO: some code goes here
        if(i<0||i> tdItems.length)
        {
            throw new NoSuchElementException("Invalid type index");
        }
        return tdItems[i].fieldType;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException if no field with a matching name is found.
     */
    public int indexForFieldName(String name) throws NoSuchElementException {//���ݸ�����������index
        // TODO: some code goes here
        for(int i=0;i< tdItems.length;i++)
        {
            if(tdItems[i].fieldName.equals(name)) {
                return i;
            }
        }
        throw new NoSuchElementException("Not find the index of"+name);//�Ҳ����׳��쳣����
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {//����Ԫ��ռ�ֽڴ�С
        // TODO: some code goes here
        int size=0;
        for (TDItem tdItem : tdItems) {
            size += tdItem.fieldType.getLen();
        }
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *
     * @param td1 The TupleDesc with the first fields of the new TupleDesc
     * @param td2 The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {//�ϲ�û��ȥ��
        // TODO: some code goes here
        if(td1==null)
        {
            return td2;
        }
        if (td2==null)
        {
            return td1;
        }
        int index=0;
        Type[] typeAr=new Type[td1.numFields()+ td2.numFields()];//�ϲ�����������
        String[] fieldAr=new String[td1.numFields()+ td2.numFields()];//�ϲ�������������
        for(int i=0;i<td1.numFields();i++)
        {
            typeAr[index]=td1.tdItems[i].fieldType;
            fieldAr[index]=td1.tdItems[i].fieldName;
            index++;
        }
        for(int i=0;i< td2.numFields();i++)
        {
            typeAr[index]=td2.tdItems[i].fieldType;
            fieldAr[index]=td2.tdItems[i].fieldName;
            index++;
        }
        return new TupleDesc(typeAr,fieldAr);
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     *
     * @param o the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {//�Ƚ��Ƿ����
        // TODO: some code goes here
        /*
          instanceof �������������ǲ�������ߵĶ����Ƿ������ұߵ����ʵ����
          ���� boolean ���������͡�
          ����Ƕ����ұ����ࣻ���������ұ������������������ʱ������true��
          ���򣬷���false��
         */
        //�Ƚ�o�Ƿ�ΪTupleDesc����������Ƿ���false
        if(!(o instanceof TupleDesc o1))
        {
            return false;
        }//��objectǿ��ת����TupleDesc
        if(o1.numFields()==this.numFields())
        {
            for(int i=0;i<this.tdItems.length;i++)
            {
                if(!(o1.tdItems[i].fieldType.equals(this.tdItems[i].fieldType)))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     *
     * @return String describing this descriptor.
     */
    public String toString() {
        // TODO: some code goes here
        StringBuilder sd=new StringBuilder();
        for(int i=0;i<tdItems.length-1;i++)
        {
            sd.append(tdItems[i].fieldType).append("(").append(tdItems[i].fieldName).append("),");
        }
        sd.append(tdItems[tdItems.length - 1].fieldType).append("(").append(tdItems[tdItems.length - 1].fieldName).append(")");
        return sd.toString();
    }
}
