package exception;

import frontend.Unit;

import java.util.List;

public class SysYException extends Exception implements Comparable<SysYException> {
    public ExceptionKind kind;
    public int line;

    public List<Unit> record;

    public SysYException(ExceptionKind kind, int line) {
        this.kind = kind;
        this.line = line;
    }

    public SysYException(ExceptionKind kind, int line, List<Unit> record) {
        this.kind = kind;
        this.line = line;
        this.record = record;
    }

    @Override
    public String toString() {
        return line + " " + kind.code;
    }

    @Override
    public int compareTo(SysYException o) {
        return Integer.compare(this.line, o.line);
    }


    public enum ExceptionKind {
        ILLEGAL_SYMBOL("a", "格式字符串中出现非法字符"),
        NAME_REDEFINITION("b", "函数名或变量名在当前作用域下重复定义"),
        UNDEFINED_NAME("c", "使用了未定义的标识符"),
        PARAM_COUNT_MISMATCH("d", "参数个数与函数定义中的参数个数不匹配"),
        PARAM_TYPE_MISMATCH("e", "参数类型与函数定义中对应位置的参数类型不匹配"),
        RETURN_TYPE_MISMATCH("f", "无返回值的函数存在不匹配的return语句"),
        MISSING_RETURN_STATEMENT("g", "有返回值的函数缺少return语句"),
        MODIFY_CONST_VALUE("h", "尝试修改一个常量"),
        MISSING_SEMICOLON("i", "缺少分号"),
        MISSING_RIGHT_PARENT("j", "缺少右小括号"),
        MISSING_RIGHT_BRACKET("k", "缺少右中括号"),
        PRINTF_ARG_MISMATCH("l", "printf中格式字符与表达式个数不匹配"),
        ERROR_BREAK_CONTINUE("m", "在非循环块中使用break和continue语句"),
        ILLEGAL_OPERATION("io", "非法的运算操作"),
        END_OF_FILE("EOF", "文件结束"),
        ERROR("unexpected error", "未知错误");
        public final String code;
        public final String description;

        ExceptionKind(String s, String description) {
            this.code = s;
            this.description = description;
        }
    }
}
