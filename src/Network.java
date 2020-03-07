import java.lang.reflect.Array;
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

	private static List<Node> nodes;
	private static HashMap<Integer, Node> node_map;
	private static List<Node> ring;
    private Semaphore netSemaphore;
    private Semaphore nodesSemaphore;

	private int round;
	private int period = 20;
	private Map<Integer, String> msgToDeliver; // Integer for the id of the sender and String for the message
    private File f_elect_fail;
    boolean elect_file_finished = false;
    boolean elect_just_called = false;

    BufferedWriter out_file;
    Node path[];

    Network(String graph, String elect) throws IOException{

        // build ring here as well
        // Node have a next pointer ?

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

            //System.out.println(node_id);

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
                if(!n.isNeighbour(prev)){
                    n.addNeighbour(prev);
                }
            }

            // set previous to be current
            prev = n;
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

        hamCycle();
        printGraph();
        printRing();
    }

    boolean isSafe(Node v, Node path[], int pos)
    {

        for(int i = 0; i < pos; i++){
            if(path[i].getNodeId() == v.getNodeId()){
                return false;
            }
        }
        return true;
    }

    /* A recursive utility function to solve hamiltonian
       cycle problem */
    boolean hamCycleUtil(Node path[], int pos)
    {

        if(pos == path.length){
            if(path[path.length-1].isNeighbour(path[0])){
                return true;
            }else{
                return false;
            }
        }

        for(Node n : path[pos-1].myNeighbours){
            // if not already in list
            if(isSafe(n, path, pos)){
                path[pos] = n;
                if(hamCycleUtil(path, pos+1) == true){
                    return true;
                }
                path[pos] = null;
            }
        }
        return false;
    }

    /* This function solves the Hamiltonian Cycle problem using
       Backtracking. It mainly uses hamCycleUtil() to solve the
       problem. It returns false if there is no Hamiltonian Cycle
       possible, otherwise return true and prints the path.
       Please note that there may be more than one solutions,
       this function prints one of the feasible solutions. */
    int hamCycle()
    {
        int V = ring.size();
        path = new Node[ring.size()];

        for (int i = 0; i < path.length; i++)
            path[i] = null;

        /* Let us put vertex 0 as the first vertex in the path.
           If there is a Hamiltonian Cycle, then the path can be
           started from any point of the cycle as the graph is
           undirected */
        path[0] = ring.get(0);
        if (hamCycleUtil(path, 1) == false)
        {
            System.out.println("\nSolution does not exist");
            return 0;
        }

        for(Node n : path ){
            if(n != null) {
                System.out.print(n.getNodeId() + " ");
            }else{
                System.out.print("null ");
            }
        }
        System.out.print("\n");

        return 1;
    }


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

    public void printRing(){
        for(Node n : ring){
            System.out.print(n.getNodeId());
            System.out.print(" ");
        }
        System.out.print('\n');
    }

    // creates new node if not in map
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

	public void NetSimulator()  throws IOException, InterruptedException {
        msgToDeliver = new HashMap<Integer, String>();
        round = 0;

        // read data into these from file
        String elect_fail = "";
        int elect_round = -1;
        ArrayList<Integer> elect_nodes = new ArrayList<Integer>();


        /*
        Code to call methods for parsing the input file, initiating the system and producing the log can be added here.
        */

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

            // System.out.println("Round " + round + " all messages received now sending messages");

            // parse elect file here

            // check at start of new round if anything needs to be elected
            if(round == (elect_round-1)){
                // initialise election for a nodes

                if(elect_fail.equals("ELECT")){

                    for(int node_id : elect_nodes){
                        Node n = node_map.get(node_id);
                        n.startElection();
                        elect_just_called = true;
                        System.out.println("Initiation of election; Round: " + elect_round + " Node: " + node_id);
                    }

                    // clear nodes
                    elect_nodes.clear();

                }else if(elect_fail.equals("FAIL")){
                    // see what happens


                }

                // check line is not blank see if has next
                if(sc.hasNextLine() && !(next_line = sc.nextLine()).isEmpty()){
                    // parse next line
                    // check if line is empty
                    line_elect = next_line.split(" ");

                    if (line_elect.length < 3) {
                        System.out.println("ERROR: malformed ELECT line: " + next_line);
                    }

                    elect_fail = line_elect[0];
                    elect_round = Integer.parseInt(line_elect[1]);

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

            // ensure all messages have arrived ?

            // elect just called stops termination problem with single ELECT message in file placed in
            if (msgToDeliver.isEmpty() && elect_file_finished == true && (elect_just_called == false)) {
                break;
            }

            // rounds proceed here

            //deliver messages

            deliverMessages();
            // time to deliver messages
            Thread.sleep(20);

            // Start of new round
            round++;
            // release after delivering messages,
            elect_just_called = false; // reset if elect just called
            nodesSemaphore.release(ring.size());
        }

        //break wait for all messages to be delivered
        System.out.println("Main thread terminated");

        for (Node n : ring){
            n.interrupt();
        }

        out_file.close();
    }
   		
   	private void parseFile(String fileName) throws IOException {
   		/*
   		Code to parse the file can be added here. Notice that the method's descriptor must be defined.
   		*/

	}

	public synchronized boolean isMsgEmpty(){
        return false;
    }
	
	public synchronized void addMessage(int id, String m) {
		/*
		At each round, the network collects all the messages that the nodes want to send to their neighbours. 
		Implement this logic here.
		*/

        msgToDeliver.put(id,m);
	}
	
	public synchronized void deliverMessages() {
		/*
		At each round, the network delivers all the messages that it has collected from the nodes.
		Implement this logic here.
		The network must ensure that a node can send only to its neighbours, one message per round per neighbour.
		*/

		// how to know when to deliver messages
        // wait for node to send to neighbours ? (bad)

        // send to nodes msg to it's neighbour in the ring
            // enforces sending only to neighbour


        //ArrayList<Integer> removeList = new ArrayList<Integer>(); // if needed

        // only receive one message per node
        for( int node_id : msgToDeliver.keySet()){

//            if(node_id == -1){
//                // election just started so message placed in delivery to stop termination
//                continue;
//            }

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
		
	public synchronized void informNodeFailure(int id) {
		/*
		Method to inform the neighbours of a failed node about the event.
		*/

	}
	
	
    public static void main(String[] args) throws IOException, InterruptedException {
        /*
        Your main must get the input file as input.
        */

        if(args.length < 2){
            System.out.println("Please provide files!");
        }else{
            System.out.format("arg[0]: %s, arg[1]: %s\n", args[0], args[1]);
        }

        // parse ds_elect
        String ds_graph = args[0];
        String ds_elect_fail = args[1];

        Network net = new Network(ds_graph, ds_elect_fail);

        // start simulator
         net.NetSimulator();

    }
}
