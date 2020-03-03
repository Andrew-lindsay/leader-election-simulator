import java.util.*;
import java.io.*;

/* Class to represent a node. Each node must run on its own thread.*/

public class Node extends Thread {

	private int id;
	private boolean participant = false;
	private boolean leader = false;
	private Network network;
	// private Node next;
    private int startedNum = 0;

	// Neighbouring nodes
	public List<Node> myNeighbours;

	// Queues for the incoming messages
	public List<String> incomingMsg;
	
	public Node(int id, Network network){
	
		this.id = id;
		this.network = network; // get network in thread ?
		
		myNeighbours = new ArrayList<Node>();
		incomingMsg = new ArrayList<String>();
	}
	
	// Basic methods for the Node class
	
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
		return false;
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
		The message must be delivered to its recepients through the network.
		This method need only implement the logic of the network receiving an outgoing message from a node.
		The remainder of the logic will be implemented in the network class.
		*/

		// access network and add to hash map
        network.addMessage(id, m);
	}

    @Override
    public void run(){

        // use barriers [Class CountDownLatch] or semaphore

        // if no message proceed to latch straight away
		while(true) {
			// logic for handling incoming messages
			// could get multiple messages from diff neighbour nodes can only send one tho ?
			// if you get multiple messages wait until next round to handle them

			System.out.println("Started thread: " + id + " for " + startedNum + " time");
			startedNum++;
			try {
				wait();
			}catch (InterruptedException e){
				e.printStackTrace();
			}
		}
    }

}
