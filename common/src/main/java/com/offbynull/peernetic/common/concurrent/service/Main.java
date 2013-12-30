package com.offbynull.peernetic.common.concurrent.service;



public final class Main {
    public static final void main(String[] args) throws Throwable {

        
        Service serviceThread = new Service() {

            @Override
            protected boolean triggerStop() {
                return false;
            }

            @Override
            protected void onStop() throws Exception {
                System.out.println("onStop");
            }

            @Override
            protected void onProcess() throws Exception {
                System.out.println("onProcess");
            }

            @Override
            protected void onStart(Object... init) throws Exception {
                System.out.println("onStart");
            }
            
        };
        
        serviceThread.startAndWait();
        serviceThread.stopAndWait();
    }
}
