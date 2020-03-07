import java.util.*;
import java.io.*;
import java.util.concurrent.Semaphore;

import static java.lang.Math.max;

/* Class to represent a node. Each node must run on its own thread.*/

public class Node extends Thread {

	private BufferedWriter outfile_writer;

	private int id;
	private boolean participant = false;
	private boolean leader = false;
	private Network network;

	// private Node next;
    private int startedNum = 0;
    private int leader_node = 0;

    // for synchronization
    private Semaphore nodesSemaphore = null;
    private Semaphore netSemaphore = null;

	// Neighbouring nodes
	public List<Node> myNeighbours;

	// Queues for the incoming messages
	public List<String> incomingMsg;

	public void setNetSemaphore(Semaphore netSemaphore) {
		this.netSemaphore = netSemaphore;
	}

	public void setNodesSemaphore(Semaphore nodesSemaphore) {
		this.nodesSemaphore = nodesSemaphore;
	}

	public Node(int id, Network network, BufferedWriter br){
		this.id = id;
		this.network = network; // get network in thread ?
		this.outfile_writer = br;

		myNeighbours = new ArrayList<Node>();
		incomingMsg = new ArrayList<String>();
	}
	
	// Basic methods for the Node class

	public void startElection(){
	    // how to handle two messages at once
        incomingMsg.add("START_ELECT " +  id);
	}

	
	public int getNodeId() {
		/*
		Method to get the Id of a node instance
		*/
		return id;
	}
			
	public boolean isNodeLeader() {
		/*
		Method to return true if the node is currently a leader
		*/
		return leader;
	}

	public List<Node> getNeighbors() {
		/*
		Method to get the neighbours of the node
		*/
		return myNeighbours;
	}

	public boolean isNeighbour(Node n){
	    boolean inlist = false;
        for(int i = 0; i < myNeighbours.size(); i++) {
            if(myNeighbours.get(i).id == n.id ){
                inlist = true;
            }
        }
        return inlist;
    }

	public void addNeighbour(Node n) {
		/*
		Method to add a neighbour to a node
		*/
        myNeighbours.add(n);
	}

	public void receiveMsg(String m) {
		/*
		Method that implements the reception of an incoming message by a node
		*/

		// add to incoming message list
        incomingMsg.add(m);
	}

	public void sendMsg(String m) {
		/*
		Method that implements the sending of a message by a node. 
		The message must be delivered to its recipients through the network.
		This method need only implement the logic of the network receiving an outgoing message from a node.
		The remainder of the logic will be implemented in the network class.
		*/

		// access network and add to hash map
        network.addMessage(id, m);
	}

	public void processMsg(){
	    // do you need to handle all messages in queue can that even happen in a ring?

	    // read from message buffer only read one message
        if(incomingMsg.size() == 0){
            return;
        }

        String msg  = incomingMsg.remove(0); // inefficient use queue
        //System.out.println(msg);

        // parse msg
        String msg_elems[] = msg.split(" ");

        if(msg_elems.length < 2){ System.out.println("Malformed message");}

        // get message type
        String msg_type = msg_elems[0];

        // handle different types of message
        if(msg_type.equals("ELECT")){
            int msg_node_id = Integer.parseInt(msg_elems[1]);
            String send_msg_str = String.format("FORWARD %d", max(id, msg_node_id));
            if(participant == false){
                participant = true;
                sendMsg(send_msg_str);
            }else if(participant == true && msg_node_id > id ){
                participant = true;
                sendMsg(send_msg_str);
            }else if(msg_node_id == id){
                // this really cannot happen as election message turns into forward message?
                leader = true;
                leader_node = id;
                participant = false;
                sendMsg("LEADER " + id);
            }else{
                // already sent message around as participant
                // do nothing as participant true and m.id < p.id

                // if another message in queue process it could result in sending a message
                    // case when ELECT is in queue
            }
        }else if(msg_type.equals("LEADER")){
            int leader_node_id = Integer.parseInt(msg_elems[1]);
            if(!isNodeLeader()) {
                this.leader_node = id;
                participant = false;
                // forward leader message
                sendMsg("LEADER " + leader_node_id);
            }
        }else if(msg_type.equals("FORWARD")){
            int msg_node_id = Integer.parseInt(msg_elems[1]);
            String send_msg_str = String.format("FORWARD %d", max(id, msg_node_id));
            if(participant == false){
                participant = true;
                sendMsg(send_msg_str);
            }else if(participant == true && id < msg_node_id){
                sendMsg(send_msg_str);
            }else if(msg_node_id == id){
                leader = true;
                leader_node = id;
                participant = false; // stop participating as now leader this might not be wanted
                sendMsg("LEADER " + this.id);

                // leader elected write to file
                synchronized (outfile_writer){
                    try {
                        outfile_writer.write("Leader Node " + this.id+ "\n");
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }

                System.out.println("Node " + this.id + " elected Leader Round: " + startedNum);
            }else{
                // do nothing
                // if other message in queue process it ?
            }

        }else if(msg_type.equals("START_ELECT")){
            participant = true;
            sendMsg("ELECT " + this.id);
        }
        else{
            System.out.println("MALFORMED MESSAGE");
        }
    }

    @Override
    public void run(){

	    try {
            // if no message proceed to latch straight away
            while (true) {
                // logic for handling incoming messages
                // could get multiple messages from diff neighbour nodes can only send one tho ?
                // if you get multiple messages wait until next round to handle them

                nodesSemaphore.acquire(); // can reach here directly after acquire again

                // sleep code
//                try {
//                    Thread.sleep(20);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                // process messaged
                processMsg();

                //System.out.println("Messages sent for thread:  " + id + " for round " + startedNum);
                startedNum++; //round count

                netSemaphore.release();

                // spin until nodesSemaphore zero still can cause bugs
                // can still cause errors if  while (nodesSemaphore.availablePermits() !=0){};
            }
        }catch (InterruptedException e){
            System.out.println("Thread "  + this.id +  " interrupted by Main Thread");
        }

    }

}
