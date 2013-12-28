package com.offbynull.peernetic.common.concurrent.service;



public final class Main {
    public static final void main(String[] args) throws Throwable {

        Service service = new ServiceAdapter() {

            @Override
            public void triggerStop() {
                System.out.println("triggerStop");
            }

            @Override
            public void onStop() throws Exception {
                System.out.println("onStop");
            }

            @Override
            public void onProcess() throws Exception {
                System.out.println("onProcess");
            }

            @Override
            public void onStart() throws Exception {
                System.out.println("onStart");
            }
            
        };
        
        ServiceThread serviceThread = new ServiceThread(service);
        serviceThread.startAndWait();
        serviceThread.stopAndWait();
    }
}
