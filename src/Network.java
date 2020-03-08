import java.util.*;
import java.io.*;
import java.util.concurrent.Semaphore;


/* 
Class to simulate the network. System design directions:

- Synchronous communication: each round lasts for 20ms
- At each round the network receives the messages that the nodes want to send and delivers them
- The network should make sure that:
	- A node can only send messages to its neighbours
	- A node can only send one message per neighbour per round
- When a node fails, the network must inform all the node's neighbours about the failure
*/
public class Network {

	private static List<Node> nodes; //
	private static HashMap<Integer, Node> node_map;
	private static ArrayList<Node> ring; // represents the ring formed
    private Semaphore netSemaphore; // lock to tell network when all threads have sent messages
    private Semaphore nodesSemaphore; // lock to tell threads when all messages of been delivered

	private int round;
	private int period = 20;
	private Map<Integer, String> msgToDeliver; // Integer for the id of the sender and String for the message
    private File f_elect_fail;
    boolean elect_file_finished = false;
    boolean elect_just_called = false;
    boolean first_fail = true;

    // handles log.txt
    BufferedWriter out_file;

    // stores the path of the new cycle discovered before replacing ring
    Node path[];

    /**
     * Constructor parses the graph file creating the graph in nodes_map and storing the ring
     * specified by the file in the Array list ring.
    **/
    Network(String graph, String elect) throws IOException{

        // initialising setup
        nodes = new ArrayList<Node>();
        msgToDeliver = new HashMap<Integer, String>();
        node_map = new HashMap<Integer, Node>();
        ring = new ArrayList<Node>();
        f_elect_fail = new File(elect);
        out_file = new BufferedWriter(new FileWriter("log.txt"));

        // setup nodes from graph here
        Scanner s_graph = new Scanner(new File(graph));

        Node prev = null;
        while(s_graph.hasNextLine()){
            String line = s_graph.nextLine();
            String[] node_line = line.split(" ");

            // get first node
            int node_id = Integer.parseInt(node_line[0]);
            Node n;

            // if not already seen node create node and add to map
            n = getNodeInMap(node_id);

            // add rest of nodes as neighbours
            Node neighbour;
            for(int i = 1; i < node_line.length; i++){
                neighbour = getNodeInMap(Integer.parseInt(node_line[i]));
                n.addNeighbour(neighbour);
            }

            // add node to ring
            ring.add(n);

            // add previous node to neighbours of current node if not already there
            // implicit in the ring
            if(prev != null){
                if(!n.hasNeighbour(prev)){
                    n.addNeighbour(prev);
                }

                if(!prev.hasNeighbour(n)){
                    prev.addNeighbour(n);
                }
            }

            // set previous to be current
            prev = n;
        }

        // add first node as neighbour of last node
        Node last = ring.get(ring.size()-1);
        Node first = ring.get(0);
        if(!last.hasNeighbour(first)){
            last.addNeighbour(first);
        }

        // last node as neighbour of first node
        if(!first.hasNeighbour(last)){
            first.addNeighbour(last);
        }

        s_graph.close();

        // semaphore for number waiting for all threads to send
        netSemaphore = new Semaphore(0,true);

        // nodes go first
        nodesSemaphore = new Semaphore(ring.size(),true);

        // setSemaphore for nodes
        for(Node x : ring){
            x.setNodesSemaphore(nodesSemaphore);
            x.setNetSemaphore(netSemaphore);
        }

        // find cycle using all nodes in the graph
        findFullCycle();

        // DEBUG
        printGraph();
        printRing();
    }

    /**
     * Debug function for printing the graph
    * */
    public void printGraph(){
        String str = "";
        for(Node n : node_map.values()){
            str =  "" + n.getNodeId() + ": ";
            for(Node neighbour : n.myNeighbours){
                str += "" + neighbour.getNodeId() + ", ";
            }
            System.out.println(str);
        }
    }

    /**
     * Debug function for printing ring
     */
    public void printRing(){
        for(Node n : ring){
            System.out.print(n.getNodeId());
            System.out.print(" ");
        }
        System.out.print('\n');
    }

    /**
     * Creates new node if not in map returning new node or node if already in map.
     *      - used in parsing the graph from the graph file
     * */
    public Node getNodeInMap(int node_id){
        Node n;
        if(!node_map.containsKey(node_id)){
            n = new Node(node_id, this, out_file);
            node_map.put(node_id, n);
        }else{
            n = node_map.get(node_id);
        }
        return n;
    }

    /**
     * Runs the simulation of the network
     *  - Reads the elect or fail file for an event
     *  - Starts all the Node threads running and waiting for there messages.
     *  - Delivers message received from nodes, using function deliverMessages()
     *  - Increments rounds
     *  - Terminates simulation if event file has finished and no messages have been sent in a round
     *      and election did not just start
     */
	public void NetSimulator()  throws IOException, InterruptedException {
        msgToDeliver = new HashMap<Integer, String>();
        round = 0;

        // read data into these from file
        String elect_fail = "";
        int elect_round = -1;
        ArrayList<Integer> elect_nodes = new ArrayList<Integer>();

        out_file.write("Part A\n");

        // start all nodes
        for(Node node : ring){
            node.start();
        }

        // get first elect message
        Scanner sc = new Scanner(f_elect_fail);
        String next_line = sc.nextLine();
        String[] line_elect = next_line.split(" ");

        if(line_elect.length < 3){
            System.out.println("ERROR: malformed ELECT line");
        }

        // change later to saving the type to variable

        elect_fail = line_elect[0];
        elect_round = Integer.parseInt(line_elect[1]);
        for(int i = 2; i < line_elect.length; i++){
            elect_nodes.add( Integer.parseInt(line_elect[i]));
        }

        while(true) {
            // wait until all threads send messages
            netSemaphore.acquire(ring.size());  // nodes have all sent messages and called netsemaphore release

            // check at start of new round if anything needs to be elected
            if(round == (elect_round-1)){
                // initialise election for a nodes

                if(elect_fail.equals("ELECT")){
                    // start election for all nodes in the election array
                    for(int node_id : elect_nodes){
                        Node n = node_map.get(node_id);
                        n.startElection();
                        elect_just_called = true;
                        System.out.println("Initiation of election; Round: " + elect_round + " Node: " + node_id);
                    }
                    // reset election node for next line to be parsed
                    elect_nodes.clear();
                }else if(elect_fail.equals("FAIL")){

                    if(first_fail){
                        out_file.write("\nPart B\n");
                        first_fail = false;
                    }

                    // single neighbour node contacted by failure detection system so starts election
                    Node failed_node = node_map.get(elect_nodes.get(0));

                    System.out.println("Round " + (round+1) + ": Node " + failed_node.getNodeId() + " Failed");

                    // remove nodes from neighbours
                        // assuming well formed adjecent list graph were links are by directional
                        // if node x has y as neighbour y has x as neighbour
                    for(Node n : failed_node.myNeighbours){
                        n.myNeighbours.remove(failed_node);
                    }

                    // remove node from ring (needs to happen before trying to find a new cycle)
                    ring.remove(failed_node);

                    // find new path
                    if( findFullCycle() == false){
                        System.out.println("Network disconnected, can't form ring: EXITING");
                        sc.close();
                        failed_node.interrupt(); // kill failed node thread
                        node_map.remove(failed_node.getNodeId());
                        break;
                    }

                    System.out.println("Building new network ring...");

                    // DEBUG
                    printGraph();

                    // set new ring as path found
                    ring = new ArrayList<Node>(Arrays.asList(path));

                    System.out.print("New ring formed: ");
                    printCyclePath();

                    // get first neighbour of failed node to start an election
                    Node neigh_n = failed_node.myNeighbours.get(0);
                    neigh_n.startElection();

                    // clear node list of nodes failed
                    elect_nodes.clear(); // nodes parse from FAIL line need cleared
                    elect_just_called = true; // ensure that network does not exit early due to no messages
                    failed_node.interrupt(); // kill failed node thread
                    node_map.remove(failed_node.getNodeId());
                }

                // check line is not blank see if has next
                if(sc.hasNextLine() && !(next_line = sc.nextLine()).isEmpty()){
                    // parse next line
                    // check if line is empty
                    line_elect = next_line.split(" ");

                    // some error checking of file line
                    if (line_elect.length < 3) {
                        System.out.println("ERROR: malformed ELECT line: " + next_line);
                    }

                    elect_fail = line_elect[0];
                    elect_round = Integer.parseInt(line_elect[1]);

                    // place all node to elect in that round in an array
                    for (int i = 2; i < line_elect.length; i++) {
                        elect_nodes.add(Integer.parseInt(line_elect[i]));
                    }

                }else{
                    // if no next line and no messages to send then terminate as file has finished
                    // check messages
                    elect_file_finished = true;
                    sc.close();
                }
            }

            // elect just called stops termination problem with single ELECT message in file placed in
            if (msgToDeliver.isEmpty() && elect_file_finished == true && (elect_just_called == false)) {
                break;
            }

            deliverMessages();

            // time to deliver messages
            Thread.sleep(period);

            // Start of new round
            round++;

            elect_just_called = false; // reset if elect just called

            // release after delivering messages,
            nodesSemaphore.release(ring.size()); // ring size change on fail
        }

        //break wait for all messages to be delivered
        System.out.println("Main thread terminated");

        // shutdown all threads
        for (Node n : ring){
            n.interrupt();
        }

        out_file.write("simulation completed\n");
        out_file.close();
    }

    /**
     * At each round, the network collects all the messages that the nodes want to send to their neighbours.
     * Implement this logic here.
     */
    public synchronized void addMessage(int id, String m) {
        msgToDeliver.put(id,m);
	}

    /**
     * At each round, the network delivers all the messages that it has collected from the nodes.
     * Ensures that a node can send only to its neighbours in the ring, one message per round per neighbour
     * as we are using a hash map.
     *
     *  Loops over node IDs in msgToDeliver hash map sending message to ring neighbour of the Node id.
     */
    public synchronized void deliverMessages() {

        // send to nodes msg to it's neighbour in the ring
            // enforces sending only to neighbour

        // only receive one message per node
        for( int node_id : msgToDeliver.keySet()){

            // get node sending message
            Node sending_n = node_map.get(node_id);

            // find node in ring get index
            int sending_node_index = ring.indexOf(sending_n);

            // get neighbour of node
            Node receiving_node = ring.get( (sending_node_index + 1) % ring.size() );

            // send message to neighbour, add to incomingMsg
            String msg_str = msgToDeliver.get(node_id);
            System.out.println("Round " +  round + ": Message from Node " + node_id + " to Node " + receiving_node.getNodeId() + " contents: " + msg_str);
            receiving_node.receiveMsg(msg_str);
        }
        msgToDeliver.clear(); // clear list of messages after sending them
	}

    /**
     * Method to inform the neighbours of a failed node about the event.
     * */
	public synchronized void informNodeFailure(int id) {

	}

    /**
     * Code to parse the file can be added here. Notice that the method's descriptor must be defined.
     */
    private void parseFile(String fileName) throws IOException {

    }

    // ========================== FINDING CYCLES ========================
    boolean isSafeToAdd(Node v, Node path[], int pos) {
        for(int i = 0; i < pos; i++){
            if(path[i].getNodeId() == v.getNodeId()){
                return false;
            }
        }
        return true;
    }

    /**
     * recursive descent down the graph with backtracking used find cycle
     * */
    boolean cycleUtil(Node path[], int pos) {
        // base case if last node is path has first node as neighbour return true
        if(pos == path.length){
            if(path[path.length-1].hasNeighbour(path[0])){
                return true;
            }else{
                return false;
            }
        }

        // search over all neighbours to test for a cycle
        for(Node n : path[pos-1].myNeighbours){
            // if not already in list add
            if(isSafeToAdd(n, path, pos)){
                path[pos] = n;
                if(cycleUtil(path, pos+1) == true){
                    return true;
                }
                path[pos] = null; // remove node from path as it failed move on to next neighbour
            }
        }
        return false;
    }

    /**
     * Function tries to find a hamiltonian cycle in the graph
     */
    boolean findFullCycle(){
        // if malformed file not all nodes in graph could be in ring but we should be given this
        int V = ring.size();
        path = new Node[ring.size()];

        for (int i = 0; i < path.length; i++) {
            path[i] = null;
        }

        path[0] = ring.get(0);

        return cycleUtil(path, 1);
    }

    /**
     * Prints cycle found in graph
     * */
    private void printCyclePath(){
        for(Node n : path ){
            if(n != null) {
                System.out.print(n.getNodeId() + " ");
            }else{
                System.out.print("null ");
            }
        }
        System.out.print("\n");
    }

    // ========================== END OF CYCLE CODE ========================

    /*
    *  Checks args and creates network the runs the simulation
    * */
    public static void main(String[] args) throws IOException, InterruptedException {

        if(args.length < 2){
            System.out.println("Please provide files!");
        }else{
            System.out.format("arg[0]: %s, arg[1]: %s\n", args[0], args[1]);
        }

        // get args
        String ds_graph = args[0];
        String ds_elect_fail = args[1];

        //check files exist
        File graph = new File(ds_graph);
        File e_f = new File(ds_elect_fail);
        if(!graph.exists()){ System.out.println("ERROR: file " + ds_graph + " does not exist!");return;}
        if(!e_f.exists()){ System.out.println("ERROR: file " + ds_elect_fail + " does not exist!");return;}

        // construct the network from graph file
        Network net = new Network(ds_graph, ds_elect_fail);

        // start simulator
         net.NetSimulator();
    }
}
