package com.learn.common.constant;

/**
 * @author coffee
 * @since 2021-06-06 10:15
 */
public class ProductConstant {
    public enum AttrEnum{

        ATTR_TYPE_BASE(1,"基本属性"),ATTR_TYPE_SALE(0,"销售属性");


        private int code;
        private String msg;
        AttrEnum(int code,String msg){
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
