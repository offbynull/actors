package com.offbynull.p2prpc.service;

import java.util.Collections;
import java.util.List;

public interface ListerService {

    Response query(int from, int to);
    
    
    public static final class Response {
        private int total;
        private List<Integer> list;

        public Response(int total, List<Integer> list) {
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
