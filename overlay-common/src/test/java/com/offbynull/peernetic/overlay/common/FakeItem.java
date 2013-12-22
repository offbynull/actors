package com.offbynull.peernetic.overlay.common;

public class FakeItem {
    public void voidRetTest() {
    }
    
    public String stringRet() {
        return "Test";
    }
    
    public String stringRetStringArg(String str) {
        return str;
    }
    
    public String stringRetObjectArg(Object obj) {
        return "" + obj;
    }
    
    public String stringRetIntegerArg(Integer i) {
        return "OBJ" + i;
    }
    
    public String stringRetPrimitiveIntegerArg(int i) {
        return "PRIM" + i;
    }

    public String stringRetIntegerArgPrimitiveIntegerArg(Integer i, int n) {
        return "OBJ" + i + "PRIM" + n;
    }
}
