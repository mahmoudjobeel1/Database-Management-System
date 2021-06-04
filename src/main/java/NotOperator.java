// to apply enumerator
// String result = NotOperator.parseOperator("op name").apply();
public enum NotOperator {
    Less("<"){
        @Override
        public String apply() {
            return ">=";
        }
    },
    LessOrEqual("<="){
        @Override
        public String apply() {
            return ">";
        }
    },
    Greater(">"){
        @Override
        public String apply() {
            return "<=";
        }
    },
    GreaterOrEqual(">="){
        @Override
        public String apply() {
            return "<";
        }
    },
    Equal("="){
        @Override
        public String apply() {
            return "!=";
        }
    },
    NotEqual("!="){
        @Override
        public String apply() {
            return "=";
        }
    };
    private final String operator;
    private NotOperator(String operator){
        this.operator=operator;
    }
    public static NotOperator parseOperator(String operator) throws DBAppException {
        for(NotOperator op:values()){
            // System.out.println(op);
            if(op.operator.equals(operator)) return op;
        }
        throw new DBAppException("There no such operator");
    }
    public abstract String apply();
}
