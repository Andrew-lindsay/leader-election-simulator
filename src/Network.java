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


    Network(String graph, String elect) throws IOException{

        // build ring here as well
        // Node have a next pointer ?

        // initialising setup
        nodes = new ArrayList<Node>();
        msgToDeliver = new HashMap<Integer, String>();
        node_map = new HashMap<Integer, Node>();
        ring = new ArrayList<Node>();
        f_elect_fail = new File(elect);

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

        // semaphore for number waiting for all threads to send
        netSemaphore = new Semaphore(1-ring.size());

        // nodes go first
        nodesSemaphore = new Semaphore(ring.size());

        // setSemaphore for nodes
        for(Node x : ring){
            x.setNodesSemaphore(nodesSemaphore);
            x.setNetSemaphore(netSemaphore);
        }

        printGraph();
        printRing();
    }

    public void printGraph(){
        String str = "";
        for(Node n : node_map.values()){
            str =  "" + n.getNodeId() + ": ";
            for(Node neightbour : n.myNeighbours){
                str += "" + neightbour.getNodeId() + ", ";
            }
            System.out.println(str);
        }
    }

    public void printRing(){
        for(Node n : ring){
            System.out.print(n.getNodeId());
            System.out.print(" ");
        }
    }

    public Node getNodeInMap(int node_id){
        Node n;
        if(!node_map.containsKey(node_id)){
            n = new Node(node_id, this);
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
        String[] line_elect = sc.nextLine().split(" ");

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
            netSemaphore.acquire();
            System.out.println("Round over all messages received now sending messages");

            // parse elect file here

            // check at start of new round if anything needs to be elected
            if(round == elect_round){
                // initialise election for a nodes

                if(elect_fail.equals("ELECT")){

                    for(int node_id : elect_nodes){
                        Node n = node_map.get(node_id);
                        n.startElection();
                    }
                    // clear nodes
                    elect_nodes.clear();

                }else if(elect_fail.equals("FAIL")){
                    // fuck this shit
                }

                if(sc.hasNextLine()){
                    // parse next line
                    line_elect = sc.nextLine().split(" ");
                    elect_fail = line_elect[0];
                    elect_round = Integer.parseInt(line_elect[1]);

                    for(int i = 2; i < line_elect.length; i++){
                        elect_nodes.add( Integer.parseInt(line_elect[i]));
                    }

                }else{
                    // if no next line and no messages to send then terminate as file has finished
                    // check messages
                    if(msgToDeliver.isEmpty()){
                        break;
                    }
                }
            }

            // rounds proceed here
            Thread.sleep(10000);

            //deliver messages
            // TODO: complete deliver messages
            deliverMessages();

            // Start of new round
            round++;
            // release after delivering messages,
            nodesSemaphore.release(ring.size());
        }

        //break wait for all messages to be delivered
    }
   		
   	private void parseFile(String fileName) throws IOException {
   		/*
   		Code to parse the file can be added here. Notice that the method's descriptor must be defined.
   		*/


	}
	
	public synchronized void addMessage(int id, String m) {
		/*
		At each round, the network collects all the messages that the nodes want to send to their neighbours. 
		Implement this logic here.
		*/


		//

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

        // for each id in msg

        // send to nodes msg to it's neighbour in the ring
            // enforces sending only to neighbour

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
