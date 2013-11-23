package com.offbynull.p2prpc.common.services;

import java.util.Collections;
import java.util.List;

public interface ListerService {

    Services listServices(int from, int to);
    String getServiceName(int id);
    
    
    public static final class Services {
        private int total;
        private List<Integer> list;

        public Services(int total, List<Integer> list) {
            this.total = total;
            this.list = list;
        }

        public int getTotal() {
            return total;
        }

        public List<Integer> getList() {
            return Collections.unmodifiableList(list);
        }
        
    }
}
