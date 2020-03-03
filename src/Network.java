import java.lang.reflect.Array;
import java.util.*;
import java.io.*;

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

//            System.out.println(node_id);

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

        /*
        Code to call methods for parsing the input file, initiating the system and producing the log can be added here.
        */

        // start all nodes

        // get first elect message
        Scanner sc = new Scanner(f_elect_fail);

        round = 0;
        while(round != 5) {

            // wait until all threads send messages semaphore 0

            // parse elect file here
            for(Node node : ring){
                node.start();
            }

            // check at start of new round if anything needs to be elected

            // rounds proceed here
            round++;
        }
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
