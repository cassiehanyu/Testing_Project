import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.AbstractMap.*;

/**
 * Created by frankgu on 2016-03-29.
 */
public class TestingProject {
    public static String bitCodeFile;
    public static Integer T_SUPPORT = 3;
    public static Integer T_CONFIDENCE = 65;
    public static final String CALLGRAPHFILE = "callgraph.txt";

    public static Map<String, functionNode> map = new HashMap<>();
    public static Set<SimpleEntry<String, String>> high_conf_pair = new HashSet<>();

    public static class functionNode{
        String functionName;
        int uses;                                           // uses - 1 = #support
        Map<String, List<String>> callMap = new HashMap<>();
        List<String> callerList = new ArrayList<>();

        public functionNode(String functionName){
            this.functionName = functionName;
        }

        public void addPair(String function, String call_function){
            callMap.computeIfAbsent(function, k -> new ArrayList<>());
            callMap.get(function).add(call_function);
        }

        public void setUses(int uses){
            this.uses = uses;
        }

        public Map<String, List<String>> getCallMap(){
            return this.callMap;
        }

        public void addCaller(String caller_func){
            callerList.add(caller_func);
        }

        public List<String> getCallerList(){
            return new ArrayList<>(this.callerList);
        }

        public int getUses(){
            return this.uses;
        }

        public int getSupport(){
            return this.uses - 1;
        }

        @Override
        public String toString(){
            return functionName;
        }

        @Override
        public int hashCode(){
             return functionName.hashCode();
        }

        @Override
        public boolean equals(Object o){
            return this.functionName.equals(o);
        }

    }
    public static void main(String argv[]){
        /*bitCodeFile = argv[0];
        if(argv.length == 3){
            T_SUPPORT = Integer.parseInt(argv[1]);
            T_CONFIDENCE = Integer.parseInt(argv[2]);
        }*/

        // Run opt-call file with specified bit code file name and create a callgraph.txt file

        // Parse out callgraph.txt
        File call_graph = new File(CALLGRAPHFILE);
        try {
            Scanner scanner = new Scanner(call_graph);
            while(scanner.hasNext()){
                String cur_line = scanner.nextLine();
                if(cur_line.startsWith("Call graph node for function:")){
                    String functionName;
                    int uses;
                    functionName = cur_line.split("'")[1];
                    uses = Integer.parseInt(cur_line.split("#uses=")[1]);

                    if(map.containsKey(functionName)){
                        map.get(functionName).setUses(uses);
                    } else {
                        functionNode fn = new functionNode(functionName);
                        fn.setUses(uses);
                        map.put(functionName, fn);
                    }

                    processNode(scanner, functionName);

                } else if(cur_line.startsWith("Call graph node <<null function>>")){    // skip null function
                    // skip all lines until reach an empty line
                    while(scanner.hasNext()){
                        if(scanner.nextLine().isEmpty()){
                            break;
                        }
                    }
                }
            }

        } catch (IOException ex){
            System.err.println("Cannot find callgraph.txt file");
        }

        // go through the resulting map and check for pairs that exceeds input confidence
        for (Object o : map.entrySet()) {
            Entry pair = (Entry) o;
            functionNode cur_fn = (functionNode) pair.getValue();
            int cur_sup = cur_fn.getSupport();
            if (cur_sup < T_SUPPORT) {
                continue;
            } else {
                Map<String, List<String>> callMap = cur_fn.getCallMap();
                for (Object o1 : callMap.entrySet()) {
                    Entry count_pair = (Entry) o1;
                    List<String> tmp_list = (List<String>) count_pair.getValue();
                    int cur_count = tmp_list.size();
                    int cur_confidence = (cur_count * 100) / cur_sup;
                    if (cur_confidence > T_CONFIDENCE && cur_confidence != 100) {
                        // add to confidence level
                        String key_fun = cur_fn.functionName;
                        String val_fun = (String) count_pair.getKey();
                        SimpleEntry entry = new SimpleEntry(key_fun, val_fun);
                        //if (high_conf_pair.contains(entry) ||
                          //      high_conf_pair.contains(new SimpleEntry(val_fun, key_fun))) {
                            // if key value in two different orders appear in the list, consider as duplicated, ignore
                            //continue;
                        //} else {
                            high_conf_pair.add(entry);
                        //}
                    }
                }
            }
        }

        // Find all bug positions
        for(SimpleEntry pair : high_conf_pair){
            String pair_key = (String)pair.getKey();
            String pair_value = (String)pair.getValue();
            functionNode cur_func_node = map.get(pair_key);
            List<String> func_caller = cur_func_node.getCallerList();
            List<String> val_func_caller = cur_func_node.getCallMap().get(pair_value);
            if(val_func_caller == null){
                System.out.println("Error could not find any correlation between A and B failed!");
            } else {
                func_caller.removeAll(val_func_caller);

                for (String aFunc_caller : func_caller) {
                    int confidence = (val_func_caller.size() * 100) / cur_func_node.getSupport();
                    System.out.println("bug: " + pair_key + " in " + aFunc_caller + ", pair: ("
                            + pair_key + ", " + pair_value + "), support: " + val_func_caller.size() + ", confidence: "
                            + confidence + ".00%");
                }
            }
        }

    }

    public static void processNode(Scanner scanner, String caller_func){
        Vector<String> cs_list = new Vector<>();

        while(scanner.hasNext()){
            String cur_line = scanner.nextLine();
            if(cur_line.matches("  CS.*")){
                if(cur_line.split("'").length > 1) {
                    cs_list.add(cur_line.split("'")[1]);
                }
            } else {
                break;
            }
        }

        // fill functionNode
        for(int i = 0; i < cs_list.size(); i++){
            String cur_fn = cs_list.get(i);
            functionNode fn;
            if(map.containsKey(cur_fn)){
                fn = map.get(cur_fn);
            } else {
                fn = new functionNode(cur_fn);
                map.put(cur_fn, fn);
            }

            // CHECK ERROR
            fn.addCaller(caller_func);

            for(int j = 0; j < cs_list.size(); j++){
                fn.addPair(cs_list.get(j), caller_func);
            }
        }
    }

}