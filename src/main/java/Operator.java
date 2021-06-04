// to apply enumerator
// boolean result = Operator.parseOperator("op name").apply(a,b);
public enum Operator {
    Less("<"){
        @Override
        public boolean apply(Comparable left, Comparable right) {
            return left.compareTo(right)<0;
        }
    },
    LessOrEqual("<="){
        @Override
        public boolean apply(Comparable left, Comparable right) {
            return left.compareTo(right)<=0;
        }
    },
    Greater(">"){
        @Override
        public boolean apply(Comparable left, Comparable right) {
            return left.compareTo(right)>0;
        }
    },
    GreaterOrEqual(">="){
        @Override
        public boolean apply(Comparable left, Comparable right) {
            return left.compareTo(right)>=0;
        }
    },
    Equal("="){
        @Override
        public boolean apply(Comparable left, Comparable right) {
            return left.compareTo(right)==0;
        }
    },
    NotEqual("!="){
        @Override
        public boolean apply(Comparable left, Comparable right) {
            return left.compareTo(right)!=0;
        }
    };
    private final String operator;
    private Operator(String operator){
        this.operator=operator;
    }
    public static Operator parseOperator(String operator) throws DBAppException {
        for(Operator op:values()){
            // System.out.println(op);
            if(op.operator.equals(operator)) return op;
        }
        throw new DBAppException("There is no such operator");
    }
    public abstract boolean apply(Comparable left,Comparable right);

}
